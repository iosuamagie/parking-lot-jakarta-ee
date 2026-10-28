package org.example.parkinglot.servlets.cars;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.HttpMethodConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.parkinglot.common.CarDto;
import org.example.parkinglot.common.Roles;
import org.example.parkinglot.ejb.CarsBean;
import org.example.parkinglot.servlets.Params;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ServletSecurity(value = @HttpConstraint(rolesAllowed = {Roles.READ_CARS}),
        httpMethodConstraints = {@HttpMethodConstraint(value = "POST", rolesAllowed = {Roles.WRITE_CARS})})
@WebServlet(name = "Cars", value = "/Cars")
public class Cars extends HttpServlet {

    @Inject
    CarsBean carsBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<CarDto> cars = carsBean.findAllCars();
        request.setAttribute("cars", cars);
        request.setAttribute("carIdsWithPhoto", carsBean.findCarIdsWithPhoto());
        request.setAttribute("numberOfFreeParkingSpots", CarsBean.TOTAL_PARKING_SPOTS - cars.size());
        request.getRequestDispatcher("/WEB-INF/pages/cars/cars.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String[] carIdsAsString = request.getParameterValues("car_ids");

        if (carIdsAsString != null) {
            List<Long> carIds = new ArrayList<>();
            for (String carIdAsString : carIdsAsString) {
                Long carId = Params.parseId(carIdAsString);
                if (carId != null) {
                    carIds.add(carId);
                }
            }
            carsBean.deleteCarsByIds(carIds);
        }
        response.sendRedirect(request.getContextPath() + "/Cars");
    }
}
