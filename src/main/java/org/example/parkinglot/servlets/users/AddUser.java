package org.example.parkinglot.servlets.users;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.parkinglot.common.Roles;
import org.example.parkinglot.ejb.UserBean;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@ServletSecurity(value = @HttpConstraint(rolesAllowed = {Roles.WRITE_USERS}))
@WebServlet(name = "AddUser", value = "/AddUser")
public class AddUser extends HttpServlet {

    @Inject
    UserBean userBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        showForm(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = UserForm.trim(request.getParameter("username"));
        String email = UserForm.trim(request.getParameter("email"));
        String password = request.getParameter("password");
        String[] userGroups = request.getParameterValues("user_groups");

        String error = UserForm.validate(username, email, password, true, null, userBean);
        if (error != null) {
            showForm(request, response, error);
            return;
        }

        userBean.createUser(username, email, password, userGroups != null ? Arrays.asList(userGroups) : List.of());

        response.sendRedirect(request.getContextPath() + "/Users");
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, String errorMessage)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", errorMessage);
        request.setAttribute("userGroups", Roles.ALL);
        request.getRequestDispatcher("/WEB-INF/pages/users/addUser.jsp").forward(request, response);
    }
}
