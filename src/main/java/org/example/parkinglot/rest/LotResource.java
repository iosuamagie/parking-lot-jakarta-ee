package org.example.parkinglot.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.example.parkinglot.common.LotSnapshotDto;
import org.example.parkinglot.ejb.ParkingSessionBean;

/** The public lot: who is parked where, the day's numbers and the latest activity. No login needed. */
@Path("/lot")
public class LotResource {

    @Inject
    ParkingSessionBean parkingSessionBean;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public LotSnapshotDto snapshot() {
        return parkingSessionBean.snapshot();
    }
}
