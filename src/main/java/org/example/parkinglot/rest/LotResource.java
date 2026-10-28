package org.example.parkinglot.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.example.parkinglot.common.LotSnapshotDto;
import org.example.parkinglot.ejb.ParkingSessionBean;

/** The public lot: who is parked where, the day's numbers and the latest activity. No login needed. */
@Path("/lot")
@Tag(name = "Lot")
public class LotResource {

    @Inject
    ParkingSessionBean parkingSessionBean;

    @GET
    @Operation(summary = "The lot now", description = "The spots, the parked cars, today's numbers and "
            + "the latest 20 entries and exits. Times are on the parking clock (60 times faster).")
    @APIResponse(responseCode = "200", description = "The state of the lot")
    @Produces(MediaType.APPLICATION_JSON)
    public LotSnapshotDto snapshot() {
        return parkingSessionBean.snapshot();
    }
}
