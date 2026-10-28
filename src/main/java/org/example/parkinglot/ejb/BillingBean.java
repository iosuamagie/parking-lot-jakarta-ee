package org.example.parkinglot.ejb;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.example.parkinglot.common.BillingSummaryDto;
import org.example.parkinglot.common.DayTotalDto;
import org.example.parkinglot.common.PageDto;
import org.example.parkinglot.common.PaidTicketDto;
import org.example.parkinglot.entities.ParkingSession;
import org.example.parkinglot.parking.AcceleratedClock;
import org.example.parkinglot.parking.TicketCodes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Money for the billing page and the PDF report. A period covers whole days in Romanian time,
 * from {@code from} 00:00 to {@code to + 1} 00:00; a ticket counts on the day its car left.
 */
@Stateless
public class BillingBean {
    public static final int PAGE_SIZE = 20;

    @PersistenceContext
    EntityManager entityManager;

    public BillingSummaryDto summary(LocalDate from, LocalDate to) {
        Map<LocalDate, int[]> byDay = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            byDay.put(day, new int[2]);
        }
        int total = 0, tickets = 0, free = 0;
        List<Object[]> rows = entityManager.createQuery(
                        "SELECT s.exitedAt, s.amountBani FROM ParkingSession s "
                                + "WHERE s.exitedAt >= :start AND s.exitedAt < :end", Object[].class)
                .setParameter("start", start(from)).setParameter("end", start(to.plusDays(1)))
                .getResultList();
        for (Object[] row : rows) {
            int amount = (Integer) row[1];
            int[] day = byDay.get(((Instant) row[0]).atZone(AcceleratedClock.ZONE).toLocalDate());
            day[0] += amount;
            day[1]++;
            total += amount;
            tickets++;
            if (amount == 0) {
                free++;
            }
        }

        long length = ChronoUnit.DAYS.between(from, to) + 1;
        Long previous = entityManager.createQuery(
                        "SELECT SUM(s.amountBani) FROM ParkingSession s WHERE s.exitedAt >= :start AND s.exitedAt < :end",
                        Long.class)
                .setParameter("start", start(from.minusDays(length))).setParameter("end", start(from))
                .getSingleResult();

        List<DayTotalDto> days = byDay.entrySet().stream()
                .map(e -> new DayTotalDto(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
        return new BillingSummaryDto(from, to, total, previous == null ? 0 : previous.intValue(), tickets,
                tickets == 0 ? 0 : Math.round((float) total / tickets), free, days);
    }

    /** Newest first; {@code plateContains} ignores upper/lower case and dashes ("sb47" finds SB-47-XYZ). */
    public PageDto<PaidTicketDto> paidTickets(LocalDate from, LocalDate to, String plateContains, int page) {
        String search = normalize(plateContains);
        String where = " FROM ParkingSession s WHERE s.exitedAt >= :start AND s.exitedAt < :end"
                + (search.isEmpty() ? "" : " AND FUNCTION('REPLACE', s.licensePlate, '-', '') LIKE :plate");

        TypedQuery<Long> count = entityManager.createQuery("SELECT COUNT(s)" + where, Long.class);
        TypedQuery<ParkingSession> items = entityManager.createQuery(
                "SELECT s" + where + " ORDER BY s.exitedAt DESC", ParkingSession.class);
        for (TypedQuery<?> query : List.of(count, items)) {
            query.setParameter("start", start(from)).setParameter("end", start(to.plusDays(1)));
            if (!search.isEmpty()) {
                query.setParameter("plate", "%" + search + "%");
            }
        }
        // Huge page numbers would overflow the row offset
        int safePage = Math.min(Math.max(1, page), Integer.MAX_VALUE / PAGE_SIZE);
        List<PaidTicketDto> found = items.setFirstResult((safePage - 1) * PAGE_SIZE).setMaxResults(PAGE_SIZE)
                .getResultList().stream().map(BillingBean::toPaid).toList();
        return new PageDto<>(found, safePage, PAGE_SIZE, count.getSingleResult());
    }

    /** Every paid ticket of the period, newest first, for the PDF report. */
    public List<PaidTicketDto> allPaidTickets(LocalDate from, LocalDate to) {
        return new ArrayList<>(entityManager.createQuery(
                        "SELECT s FROM ParkingSession s WHERE s.exitedAt >= :start AND s.exitedAt < :end "
                                + "ORDER BY s.exitedAt DESC", ParkingSession.class)
                .setParameter("start", start(from)).setParameter("end", start(to.plusDays(1)))
                .getResultList().stream().map(BillingBean::toPaid).toList());
    }

    /** Only letters and digits, upper case, so the search text can never act as a LIKE wildcard. */
    static String normalize(String plateContains) {
        return plateContains == null ? "" : plateContains.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static Instant start(LocalDate day) {
        return day.atStartOfDay(AcceleratedClock.ZONE).toInstant();
    }

    private static PaidTicketDto toPaid(ParkingSession s) {
        return new PaidTicketDto(s.getTicketCode(), TicketCodes.displayNumber(s.getId()), s.getLicensePlate(),
                s.getSpot(), s.getEnteredAt(), s.getExitedAt(), s.getAmountBani());
    }
}
