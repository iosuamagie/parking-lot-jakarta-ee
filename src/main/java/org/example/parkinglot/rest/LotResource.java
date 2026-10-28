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
@Tag(name = "Parcare")
public class LotResource {

    @Inject
    ParkingSessionBean parkingSessionBean;

    @GET
    @Operation(summary = "Parcarea acum", description = "Locurile, mașinile parcate, cifrele zilei și "
            + "ultimele 20 de intrări și ieșiri. Orele sunt pe ceasul parcării (de 60 de ori mai rapid).")
    @APIResponse(responseCode = "200", description = "Starea parcării")
    @Produces(MediaType.APPLICATION_JSON)
    public LotSnapshotDto snapshot() {
        return parkingSessionBean.snapshot();
    }
}
