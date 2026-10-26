package org.example.parkinglot.servlets;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.parkinglot.common.CarPhotoDto;
import org.example.parkinglot.common.ImageTypes;
import org.example.parkinglot.common.Roles;
import org.example.parkinglot.ejb.CarsBean;

import java.io.IOException;

@ServletSecurity(value = @HttpConstraint(rolesAllowed = {Roles.READ_CARS}))
@WebServlet(name = "CarPhotos", value = "/CarPhotos")
public class CarPhotos extends HttpServlet {

    @Inject
    CarsBean carsBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long carId = Params.parseId(request.getParameter("id"));
        if (carId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        CarPhotoDto photo = carsBean.findPhotoByCarId(carId);

        // Only serve real images, so an old upload with another type can't run as HTML in the browser
        String fileType = photo == null ? null : ImageTypes.detect(photo.getFileContent());
        if (fileType == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(fileType);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLength(photo.getFileContent().length);
        response.getOutputStream().write(photo.getFileContent());
    }
}
