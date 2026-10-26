package org.example.parkinglot.servlets.cars;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.parkinglot.common.CarDto;
import org.example.parkinglot.common.Roles;
import org.example.parkinglot.common.UserDto;
import org.example.parkinglot.ejb.CarsBean;
import org.example.parkinglot.ejb.UserBean;
import org.example.parkinglot.servlets.Params;

import java.io.IOException;

@ServletSecurity(value = @HttpConstraint(rolesAllowed = {Roles.WRITE_CARS}))
@WebServlet(name = "EditCar", value = "/EditCar")
public class EditCar extends HttpServlet {

    @Inject
    CarsBean carsBean;

    @Inject
    UserBean userBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long carId = Params.parseId(request.getParameter("id"));
        CarDto car = carId == null ? null : carsBean.findById(carId);
        if (car == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        showForm(request, response, car, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String licensePlate = CarForm.trim(request.getParameter("license_plate"));
        String parkingSpot = CarForm.trim(request.getParameter("parking_spot"));
        Long userId = Params.parseId(request.getParameter("owner_id"));
        Long carId = Params.parseId(request.getParameter("car_id"));
        if (carId == null || carsBean.findById(carId) == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String error = CarForm.validate(licensePlate, parkingSpot, userId, carId, carsBean);
        if (error == null && !carsBean.updateCar(carId, licensePlate, parkingSpot, userId)) {
            error = "The selected owner no longer exists.";
        }
        if (error != null) {
            // Show the form again with what the user typed
            UserDto owner = userId == null ? null : userBean.findById(userId);
            CarDto typed = new CarDto(carId, licensePlate, parkingSpot, owner == null ? "" : owner.getUsername());
            showForm(request, response, typed, error);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/Cars");
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, CarDto car, String errorMessage)
            throws ServletException, IOException {
        request.setAttribute("car", car);
        request.setAttribute("errorMessage", errorMessage);
        request.setAttribute("users", userBean.findAllUsers());
        request.getRequestDispatcher("/WEB-INF/pages/cars/editCar.jsp").forward(request, response);
    }
}
