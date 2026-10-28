package org.example.parkinglot.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.example.parkinglot.common.TicketDto;
import org.example.parkinglot.documents.QrSvg;
import org.example.parkinglot.documents.ReceiptPdf;
import org.example.parkinglot.ejb.ParkingSessionBean;

/**
 * Public tickets: whoever has the (unguessable) ticket code can see the ticket, like with a paper ticket.
 */
@Path("/tickets")
@Tag(name = "Tickets")
public class TicketsResource {
    private static final String SVG = "image/svg+xml";
    private static final String PDF = "application/pdf";

    @Inject
    ParkingSessionBean parkingSessionBean;

    @Context
    UriInfo uriInfo;

    @GET
    @Path("/{code}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "A ticket", description = "While the car is parked, exitedAt and amountBani are missing; "
            + "the amount so far follows from clockNow and the tariff.")
    @APIResponse(responseCode = "200", description = "The ticket")
    @APIResponse(responseCode = "404", description = "The code is not valid or there is no such ticket")
    public TicketDto find(
            @Parameter(description = "The ticket code, 22 characters", example = "o2dnnRdS1JKEc7OESPdvvA")
            @PathParam("code") String code) {
        return parkingSessionBean.findTicket(code).orElseThrow(NotFoundException::new);
    }

    /** The QR code opens the ticket page on a phone. */
    @GET
    @Path("/{code}/qr.svg")
    @Produces(SVG)
    @Operation(summary = "The ticket's QR code", description = "An SVG that opens the page /ticket/{code}.")
    @APIResponse(responseCode = "200", description = "Imaginea SVG")
    @APIResponse(responseCode = "404", description = "The code is not valid or there is no such ticket")
    public Response qr(
            @Parameter(description = "The ticket code, 22 characters", example = "o2dnnRdS1JKEc7OESPdvvA")
            @PathParam("code") String code) {
        TicketDto ticket = find(code);
        CacheControl cache = new CacheControl();
        cache.setMaxAge(24 * 60 * 60);
        return Response.ok(QrSvg.svg(publicUrl() + "/ticket/" + ticket.ticketCode()), SVG)
                .cacheControl(cache)
                .build();
    }

    /** The receipt exists only after the car has left and paid. */
    @GET
    @Path("/{code}/receipt.pdf")
    @Produces({PDF, MediaType.TEXT_PLAIN})
    @Operation(summary = "The PDF receipt", description = "Only exists after the car has left.")
    @APIResponse(responseCode = "200", description = "The receipt, 80 mm wide")
    @APIResponse(responseCode = "404", description = "The code is not valid or there is no such ticket")
    @APIResponse(responseCode = "409", description = "The car is still parked")
    public Response receipt(
            @Parameter(description = "The ticket code, 22 characters", example = "o2dnnRdS1JKEc7OESPdvvA")
            @PathParam("code") String code) {
        TicketDto ticket = find(code);
        if (ticket.exitedAt() == null) {
            return Response.status(Response.Status.CONFLICT)
                    .type(MediaType.TEXT_PLAIN_TYPE.withCharset("UTF-8"))
                    .entity("The receipt is ready once the car has left")
                    .build();
        }
        return Response.ok(ReceiptPdf.render(ticket), PDF)
                .header("Content-Disposition", "attachment; filename=\"chitanta-" + ticket.ticketNumber() + ".pdf\"")
                .build();
    }

    /**
     * The address phones should open: PARKPULSE_PUBLIC_URL when the app runs behind another address,
     * otherwise the address this request came to.
     */
    private String publicUrl() {
        String configured = System.getenv("PARKPULSE_PUBLIC_URL");
        String url = configured != null && !configured.isBlank()
                ? configured.trim()
                : uriInfo.getBaseUri().resolve("..").toString();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
