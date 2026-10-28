package org.example.parkinglot.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.example.parkinglot.common.CarDto;
import org.example.parkinglot.ejb.CarsBean;

import java.util.List;

/**
 * Read-only JSON API for the cars. Access needs the READ_CARS role, set as a security constraint in web.xml.
 */
@Path("/cars")
@Tag(name = "Cars")
public class CarsController {

    @Inject
    CarsBean carsBean;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "All subscription cars")
    @APIResponse(responseCode = "200", description = "The cars")
    public List<CarDto> findAll() {
        return carsBean.findAllCars();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "One car")
    @APIResponse(responseCode = "200", description = "The car")
    @APIResponse(responseCode = "404", description = "No such car")
    public CarDto findById(@Parameter(description = "The car's id", example = "1") @PathParam("id") Long id) {
        CarDto car = carsBean.findById(id);
        if (car == null) {
            throw new NotFoundException();
        }
        return car;
    }
}
