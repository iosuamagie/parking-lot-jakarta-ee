package org.example.parkinglot.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.parkinglot.common.PageDto;
import org.example.parkinglot.common.PaidTicketDto;
import org.example.parkinglot.documents.ReportPdf;
import org.example.parkinglot.ejb.BillingBean;
import org.example.parkinglot.ejb.ParkingClockBean;
import org.example.parkinglot.parking.AcceleratedClock;
import org.example.parkinglot.parking.BillingPeriod;
import org.example.parkinglot.servlets.Params;

/**
 * Paid tickets and the PDF report. Access needs the INVOICING role, set as a security constraint in web.xml.
 * Dates are written as 2026-10-27; without dates the period is the last 7 days.
 */
@Path("/billing")
public class BillingResource {

    @Inject
    BillingBean billingBean;

    @Inject
    ParkingClockBean clock;

    @GET
    @Path("/tickets")
    @Produces(MediaType.APPLICATION_JSON)
    public PageDto<PaidTicketDto> tickets(@QueryParam("from") String from, @QueryParam("to") String to,
                                          @QueryParam("plate") String plate,
                                          @QueryParam("page") String page) {
        BillingPeriod period = period(from, to);
        Long pageNumber = Params.parseId(page);
        return billingBean.paidTickets(period.from(), period.to(), plate,
                pageNumber == null ? 1 : (int) Math.min(pageNumber, Integer.MAX_VALUE));
    }

    @GET
    @Path("/report.pdf")
    @Produces("application/pdf")
    public Response report(@QueryParam("from") String from, @QueryParam("to") String to) {
        BillingPeriod period = period(from, to);
        byte[] pdf = ReportPdf.render(billingBean.summary(period.from(), period.to()),
                billingBean.allPaidTickets(period.from(), period.to()));
        return Response.ok(pdf, "application/pdf")
                .header("Content-Disposition",
                        "attachment; filename=\"raport-incasari-" + period.from() + "-" + period.to() + ".pdf\"")
                .build();
    }

    private BillingPeriod period(String from, String to) {
        try {
            return BillingPeriod.parse(from, to, clock.now().atZone(AcceleratedClock.ZONE).toLocalDate());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN_TYPE.withCharset("UTF-8"))
                    .entity(ex.getMessage())
                    .build());
        }
    }
}
