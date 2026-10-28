package org.example.parkinglot.servlets;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.parkinglot.common.BillingSummaryDto;
import org.example.parkinglot.common.DayTotalDto;
import org.example.parkinglot.common.PageDto;
import org.example.parkinglot.common.PaidTicketDto;
import org.example.parkinglot.common.Roles;
import org.example.parkinglot.ejb.BillingBean;
import org.example.parkinglot.ejb.ParkingClockBean;
import org.example.parkinglot.parking.AcceleratedClock;
import org.example.parkinglot.parking.BillingPeriod;
import org.example.parkinglot.parking.Tariff;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The billing page: money per period, a bar per day and the paid tickets. Only for the INVOICING role.
 * The page gets plain maps and strings, already formatted the Romanian way.
 */
@ServletSecurity(value = @HttpConstraint(rolesAllowed = {Roles.INVOICING}))
@WebServlet(name = "Billing", value = "/billing")
public class Billing extends HttpServlet {
    private static final List<String> PERIODS = List.of("today", "7d", "month", "custom");
    private static final String[] WEEKDAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    @Inject
    BillingBean billingBean;

    @Inject
    ParkingClockBean clock;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LocalDate today = clock.now().atZone(AcceleratedClock.ZONE).toLocalDate();
        // List.of(...).contains(null) throws, so a missing parameter is checked first
        String requested = request.getParameter("period");
        String periodKey = requested != null && PERIODS.contains(requested) ? requested : "7d";
        BillingPeriod period;
        try {
            period = switch (periodKey) {
                case "today" -> new BillingPeriod(today, today);
                case "month" -> new BillingPeriod(today.withDayOfMonth(1), today);
                case "custom" -> BillingPeriod.parse(request.getParameter("from"), request.getParameter("to"), today);
                default -> BillingPeriod.parse(null, null, today);
            };
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }
        String plate = request.getParameter("plate") == null ? "" : request.getParameter("plate").trim();
        Long pageParam = Params.parseId(request.getParameter("page"));
        int page = pageParam == null ? 1 : (int) Math.min(pageParam, Integer.MAX_VALUE);

        BillingSummaryDto summary = billingBean.summary(period.from(), period.to());
        PageDto<PaidTicketDto> tickets = billingBean.paidTickets(period.from(), period.to(), plate, page);

        request.setAttribute("periodKey", periodKey);
        request.setAttribute("from", period.from().toString());
        request.setAttribute("to", period.to().toString());
        request.setAttribute("plate", plate);
        request.setAttribute("kpi", kpis(summary));
        request.setAttribute("bars", bars(summary, today));
        request.setAttribute("rows", tickets.items().stream().map(Billing::row).toList());
        request.setAttribute("page", tickets.page());
        request.setAttribute("pages", tickets.pages());
        request.setAttribute("total", tickets.total());

        String base = "period=" + periodKey
                + ("custom".equals(periodKey) ? "&from=" + period.from() + "&to=" + period.to() : "");
        String search = plate.isEmpty() ? "" : "&plate=" + URLEncoder.encode(plate, StandardCharsets.UTF_8);
        request.setAttribute("prevQuery", tickets.page() > 1 ? base + search + "&page=" + (tickets.page() - 1) : null);
        request.setAttribute("nextQuery",
                tickets.page() < tickets.pages() ? base + search + "&page=" + (tickets.page() + 1) : null);
        request.setAttribute("reportQuery", "from=" + period.from() + "&to=" + period.to());

        request.getRequestDispatcher("/WEB-INF/pages/billing.jsp").forward(request, response);
    }

    private static Map<String, Object> kpis(BillingSummaryDto summary) {
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("total", Tariff.format(summary.totalBani()));
        kpi.put("paid", summary.paidTickets());
        kpi.put("average", Tariff.format(summary.averageBani()));
        kpi.put("free", summary.freeTickets());
        int previous = summary.previousPeriodTotalBani();
        if (previous > 0) {
            long change = Math.round(100.0 * (summary.totalBani() - previous) / previous);
            kpi.put("trend", (change >= 0 ? "▲ " : "▼ ") + Math.abs(change) + "% vs the previous period");
            kpi.put("trendUp", change >= 0);
        }
        return kpi;
    }

    private static List<Map<String, Object>> bars(BillingSummaryDto summary, LocalDate today) {
        int max = summary.days().stream().mapToInt(DayTotalDto::totalBani).max().orElse(0);
        boolean shortPeriod = summary.days().size() <= 14;
        return summary.days().stream().map(day -> {
            Map<String, Object> bar = new LinkedHashMap<>();
            boolean isToday = day.day().equals(today);
            String label = shortPeriod ? WEEKDAYS[day.day().getDayOfWeek().ordinal()] : String.valueOf(day.day().getDayOfMonth());
            bar.put("label", isToday ? "Today" : label);
            bar.put("height", max == 0 ? 0 : Math.max(2, Math.round(100f * day.totalBani() / max)));
            bar.put("today", isToday);
            bar.put("title", DAY_MONTH.format(day.day()) + ": " + Tariff.format(day.totalBani()) + ", " + day.tickets() + " tickets");
            return bar;
        }).toList();
    }

    private static Map<String, Object> row(PaidTicketDto ticket) {
        ZonedDateTime in = ticket.enteredAt().atZone(AcceleratedClock.ZONE);
        ZonedDateTime out = ticket.exitedAt().atZone(AcceleratedClock.ZONE);
        long minutes = Duration.between(ticket.enteredAt(), ticket.exitedAt()).toMinutes();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", ticket.ticketCode());
        row.put("number", ticket.ticketNumber());
        row.put("plate", ticket.plate());
        row.put("spot", ticket.spot());
        row.put("times", DAY_MONTH.format(in) + " " + TIME.format(in) + " → "
                + (in.toLocalDate().equals(out.toLocalDate()) ? "" : DAY_MONTH.format(out) + " ") + TIME.format(out));
        row.put("duration", minutes < 60 ? minutes + "m" : (minutes / 60) + "h " + (minutes % 60) + "m");
        row.put("amount", ticket.amountBani() == 0 ? "free" : Tariff.format(ticket.amountBani()));
        row.put("free", ticket.amountBani() == 0);
        return row;
    }
}
