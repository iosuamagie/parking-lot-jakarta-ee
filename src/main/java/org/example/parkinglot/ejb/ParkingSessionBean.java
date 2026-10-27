package org.example.parkinglot.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.parkinglot.common.ActivityDto;
import org.example.parkinglot.common.LotStatsDto;
import org.example.parkinglot.common.ParkedCarDto;
import org.example.parkinglot.common.TicketDto;
import org.example.parkinglot.entities.ParkingSession;
import org.example.parkinglot.parking.AcceleratedClock;
import org.example.parkinglot.parking.CarModel;
import org.example.parkinglot.parking.Spots;
import org.example.parkinglot.parking.Tariff;
import org.example.parkinglot.parking.TicketCodes;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Opens and closes parking sessions and answers what the live page and the tickets need. */
@Stateless
public class ParkingSessionBean {

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    ParkingClockBean clock;

    public ParkedCarDto enter(String plate, String spot, CarModel model, String color, boolean permit) {
        if (!Spots.isValid(spot)) {
            throw new IllegalStateException("There is no spot " + spot);
        }
        if (findOpenOnSpot(spot).isPresent()) {
            throw new IllegalStateException("Spot " + spot + " is taken");
        }
        boolean alreadyInside = !entityManager.createQuery(
                        "SELECT s.id FROM ParkingSession s WHERE s.licensePlate = :plate AND s.exitedAt IS NULL",
                        Long.class)
                .setParameter("plate", plate).setMaxResults(1).getResultList().isEmpty();
        if (alreadyInside) {
            throw new IllegalStateException(plate + " is already in the lot");
        }

        ParkingSession session = new ParkingSession();
        session.setTicketCode(TicketCodes.newCode());
        session.setLicensePlate(plate);
        session.setSpot(spot);
        session.setCarModel(model);
        session.setCarColor(color);
        session.setAccessiblePermit(permit);
        session.setEnteredAt(clock.now());
        entityManager.persist(session);
        entityManager.flush();
        return toParkedCar(session);
    }

    /** Closes the open session on this spot and charges it. */
    public ActivityDto exit(String spot) {
        ParkingSession session = findOpenOnSpot(spot)
                .orElseThrow(() -> new IllegalStateException("There is no car on spot " + spot));
        Instant now = clock.now();
        session.setExitedAt(now);
        session.setAmountBani(Tariff.amountBani(Duration.between(session.getEnteredAt(), now)));
        return toExited(session);
    }

    /** Saves ready-made sessions (the demo history) in one transaction. */
    public void importHistory(List<ParkingSession> history) {
        history.forEach(entityManager::persist);
    }

    public long countSessions() {
        return entityManager.createQuery("SELECT COUNT(s) FROM ParkingSession s", Long.class).getSingleResult();
    }

    public List<ParkedCarDto> findParked() {
        return entityManager.createQuery(
                        "SELECT s FROM ParkingSession s WHERE s.exitedAt IS NULL ORDER BY s.spot", ParkingSession.class)
                .getResultList().stream().map(ParkingSessionBean::toParkedCar).toList();
    }

    public Optional<TicketDto> findTicket(String code) {
        if (!TicketCodes.looksValid(code)) {
            return Optional.empty();
        }
        return entityManager.createQuery(
                        "SELECT s FROM ParkingSession s WHERE s.ticketCode = :code", ParkingSession.class)
                .setParameter("code", code).getResultList().stream().findFirst()
                .map(this::toTicket);
    }

    public LotStatsDto stats() {
        Instant now = clock.now();
        ZonedDateTime localNow = now.atZone(AcceleratedClock.ZONE);
        Instant todayStart = localNow.toLocalDate().atStartOfDay(AcceleratedClock.ZONE).toInstant();
        Instant yesterdayStart = localNow.toLocalDate().minusDays(1).atStartOfDay(AcceleratedClock.ZONE).toInstant();
        Instant yesterdaySameTime = localNow.minusDays(1).toInstant();

        long parked = entityManager.createQuery(
                "SELECT COUNT(s) FROM ParkingSession s WHERE s.exitedAt IS NULL", Long.class).getSingleResult();
        long carsToday = entityManager.createQuery(
                        "SELECT COUNT(s) FROM ParkingSession s WHERE s.enteredAt >= :start", Long.class)
                .setParameter("start", todayStart).getSingleResult();

        List<ParkingSession> closedToday = closedBetween(todayStart, now);
        int revenueToday = closedToday.stream().mapToInt(ParkingSession::getAmountBani).sum();
        int revenueYesterday = closedBetween(yesterdayStart, yesterdaySameTime).stream()
                .mapToInt(ParkingSession::getAmountBani).sum();
        long averageStay = Math.round(closedToday.stream()
                .mapToLong(s -> Duration.between(s.getEnteredAt(), s.getExitedAt()).toMinutes())
                .average().orElse(0));

        return new LotStatsDto(Spots.CAPACITY, Spots.CAPACITY - (int) parked, revenueToday, revenueYesterday,
                (int) carsToday, averageStay);
    }

    /** The latest entries and exits together, newest first. */
    public List<ActivityDto> recentActivity(int limit) {
        List<ActivityDto> activity = new ArrayList<>();
        entityManager.createQuery("SELECT s FROM ParkingSession s ORDER BY s.enteredAt DESC", ParkingSession.class)
                .setMaxResults(limit).getResultList()
                .forEach(s -> activity.add(new ActivityDto(ActivityDto.ENTERED, s.getLicensePlate(), s.getSpot(),
                        s.getTicketCode(), null, s.getEnteredAt())));
        entityManager.createQuery(
                        "SELECT s FROM ParkingSession s WHERE s.exitedAt IS NOT NULL ORDER BY s.exitedAt DESC",
                        ParkingSession.class)
                .setMaxResults(limit).getResultList()
                .forEach(s -> activity.add(toExited(s)));
        return activity.stream()
                .sorted(Comparator.comparing(ActivityDto::at).reversed())
                .limit(limit)
                .toList();
    }

    private Optional<ParkingSession> findOpenOnSpot(String spot) {
        return entityManager.createQuery(
                        "SELECT s FROM ParkingSession s WHERE s.spot = :spot AND s.exitedAt IS NULL",
                        ParkingSession.class)
                .setParameter("spot", spot).getResultList().stream().findFirst();
    }

    private List<ParkingSession> closedBetween(Instant from, Instant to) {
        return entityManager.createQuery(
                        "SELECT s FROM ParkingSession s WHERE s.exitedAt >= :from AND s.exitedAt <= :to",
                        ParkingSession.class)
                .setParameter("from", from).setParameter("to", to).getResultList();
    }

    private static ParkedCarDto toParkedCar(ParkingSession s) {
        return new ParkedCarDto(s.getSpot(), s.getLicensePlate(), s.getTicketCode(),
                TicketCodes.displayNumber(s.getId()), s.getCarModel(), s.getCarColor(), s.isAccessiblePermit(),
                s.getEnteredAt());
    }

    private static ActivityDto toExited(ParkingSession s) {
        return new ActivityDto(ActivityDto.EXITED, s.getLicensePlate(), s.getSpot(), s.getTicketCode(),
                s.getAmountBani(), s.getExitedAt());
    }

    private TicketDto toTicket(ParkingSession s) {
        return new TicketDto(s.getTicketCode(), TicketCodes.displayNumber(s.getId()), s.getLicensePlate(),
                s.getSpot(), s.getCarModel(), s.getCarColor(), s.isAccessiblePermit(), s.getEnteredAt(),
                s.getExitedAt(), s.getAmountBani(), clock.now(), clock.speed());
    }
}
