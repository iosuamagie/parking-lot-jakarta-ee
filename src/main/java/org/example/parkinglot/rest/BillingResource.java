package org.example.parkinglot.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
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
@Tag(name = "Billing")
public class BillingResource {

    @Inject
    BillingBean billingBean;

    @Inject
    ParkingClockBean clock;

    @GET
    @Path("/tickets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Paid tickets", description = "20 per page, newest first. "
            + "Without dates: the last 7 days.")
    @APIResponse(responseCode = "200", description = "A page of tickets")
    @APIResponse(responseCode = "400", description = "The dates are not a period of at most 366 days")
    public PageDto<PaidTicketDto> tickets(
            @Parameter(description = "First day, YYYY-MM-DD", example = "2026-10-21") @QueryParam("from") String from,
            @Parameter(description = "Last day, YYYY-MM-DD", example = "2026-10-27") @QueryParam("to") String to,
            @Parameter(description = "Part of the plate number", example = "SB") @QueryParam("plate") String plate,
            @Parameter(description = "The page, from 1", example = "1") @QueryParam("page") String page) {
        BillingPeriod period = period(from, to);
        Long pageNumber = Params.parseId(page);
        return billingBean.paidTickets(period.from(), period.to(), plate,
                pageNumber == null ? 1 : (int) Math.min(pageNumber, Integer.MAX_VALUE));
    }

    @GET
    @Path("/report.pdf")
    @Produces("application/pdf")
    @Operation(summary = "PDF report for a period")
    @APIResponse(responseCode = "200", description = "The report, on A4 pages")
    @APIResponse(responseCode = "400", description = "The dates are not a period of at most 366 days")
    public Response report(
            @Parameter(description = "First day, YYYY-MM-DD", example = "2026-10-21") @QueryParam("from") String from,
            @Parameter(description = "Last day, YYYY-MM-DD", example = "2026-10-27") @QueryParam("to") String to) {
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
