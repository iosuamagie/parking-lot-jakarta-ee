package org.example.parkinglot.servlets;

import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.parkinglot.common.Roles;

import java.io.IOException;

/**
 * Where the "Autentificare" button leads. Being protected, it starts the login, and after it sends each
 * user to the first page their roles allow.
 */
@ServletSecurity(value = @HttpConstraint(rolesAllowed = {
        Roles.READ_CARS, Roles.WRITE_CARS, Roles.READ_USERS, Roles.WRITE_USERS, Roles.INVOICING}))
@WebServlet(name = "Account", value = "/Account")
public class Account extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String page;
        if (request.isUserInRole(Roles.INVOICING)) {
            page = "/billing";
        } else if (request.isUserInRole(Roles.READ_CARS)) {
            page = "/Cars";
        } else if (request.isUserInRole(Roles.READ_USERS)) {
            page = "/Users";
        } else {
            page = "/";
        }
        response.sendRedirect(request.getContextPath() + page);
    }
}
