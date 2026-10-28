package org.example.parkinglot.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.example.parkinglot.ejb.ParkingSessionBean;
import org.example.parkinglot.live.LiveHub;

/**
 * The live feed of the lot as server-sent events: first a "snapshot", then "entered", "exited" and "stats".
 */
@Path("/events")
@Tag(name = "Parcare")
public class LiveEventsResource {

    @Inject
    LiveHub liveHub;

    @Inject
    ParkingSessionBean parkingSessionBean;

    @GET
    @Operation(summary = "Fluxul live (server-sent events)", description = "O conexiune care rămâne deschisă: "
            + "întâi un eveniment snapshot, apoi entered, exited și stats la fiecare mișcare. "
            + "Butonul Try it nu poate afișa un flux care nu se termină; deschide adresa direct în browser.")
    @APIResponse(responseCode = "200", description = "Fluxul de evenimente (text/event-stream)")
    @APIResponse(responseCode = "503", description = "Sunt deja prea multe pagini deschise")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@Context SseEventSink sink, @Context Sse sse) {
        if (!liveHub.subscribe(sink, sse, parkingSessionBean.snapshot())) {
            sink.close();
            throw new ServiceUnavailableException(30L);
        }
    }
}
