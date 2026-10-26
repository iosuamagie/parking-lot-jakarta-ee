package org.example.parkinglot.servlets.cars;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.parkinglot.common.Roles;
import org.example.parkinglot.ejb.CarsBean;
import org.example.parkinglot.ejb.UserBean;
import org.example.parkinglot.servlets.Params;

import java.io.IOException;

@ServletSecurity(value = @HttpConstraint(rolesAllowed = {Roles.WRITE_CARS}))
@WebServlet(name = "AddCar", value = "/AddCar")
public class AddCar extends HttpServlet {
    @Inject
    UserBean userBean;

    @Inject
    CarsBean carsBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        showForm(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (carsBean.countCars() >= CarsBean.TOTAL_PARKING_SPOTS) {
            showForm(request, response, "Parking lot is full! You cannot add more cars.");
            return;
        }

        String licensePlate = CarForm.trim(request.getParameter("license_plate"));
        String parkingSpot = CarForm.trim(request.getParameter("parking_spot"));
        Long userId = Params.parseId(request.getParameter("owner_id"));

        String error = CarForm.validate(licensePlate, parkingSpot, userId, null, carsBean);
        if (error == null && !carsBean.createCar(licensePlate, parkingSpot, userId)) {
            error = "The selected owner no longer exists.";
        }
        if (error != null) {
            showForm(request, response, error);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/Cars");
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, String errorMessage)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", errorMessage);
        request.setAttribute("users", userBean.findAllUsers());
        request.getRequestDispatcher("/WEB-INF/pages/cars/addCar.jsp").forward(request, response);
    }
}
