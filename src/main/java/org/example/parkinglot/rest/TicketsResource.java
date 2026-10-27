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
import org.example.parkinglot.common.TicketDto;
import org.example.parkinglot.documents.QrSvg;
import org.example.parkinglot.ejb.ParkingSessionBean;

/**
 * Public tickets: whoever has the (unguessable) ticket code can see the ticket, like with a paper ticket.
 */
@Path("/tickets")
public class TicketsResource {
    private static final String SVG = "image/svg+xml";

    @Inject
    ParkingSessionBean parkingSessionBean;

    @Context
    UriInfo uriInfo;

    @GET
    @Path("/{code}")
    @Produces(MediaType.APPLICATION_JSON)
    public TicketDto find(@PathParam("code") String code) {
        return parkingSessionBean.findTicket(code).orElseThrow(NotFoundException::new);
    }

    /** The QR code opens the ticket page on a phone. */
    @GET
    @Path("/{code}/qr.svg")
    @Produces(SVG)
    public Response qr(@PathParam("code") String code) {
        TicketDto ticket = find(code);
        CacheControl cache = new CacheControl();
        cache.setMaxAge(24 * 60 * 60);
        return Response.ok(QrSvg.svg(publicUrl() + "/bilet/" + ticket.ticketCode()), SVG)
                .cacheControl(cache)
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
