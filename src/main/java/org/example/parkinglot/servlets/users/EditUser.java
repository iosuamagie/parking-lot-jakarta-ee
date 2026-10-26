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
import org.example.parkinglot.common.UserDto;
import org.example.parkinglot.ejb.UserBean;
import org.example.parkinglot.servlets.Params;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@ServletSecurity(value = @HttpConstraint(rolesAllowed = {Roles.WRITE_USERS}))
@WebServlet(name = "EditUser", value = "/EditUser")
public class EditUser extends HttpServlet {

    @Inject
    UserBean userBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long userId = Params.parseId(request.getParameter("id"));
        UserDto user = userId == null ? null : userBean.findById(userId);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        showForm(request, response, user, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long userId = Params.parseId(request.getParameter("user_id"));
        String username = UserForm.trim(request.getParameter("username"));
        String email = UserForm.trim(request.getParameter("email"));
        String password = request.getParameter("password");
        String[] groupsAsArray = request.getParameterValues("user_groups");
        List<String> groups = groupsAsArray != null ? Arrays.asList(groupsAsArray) : List.of();
        if (userId == null || userBean.findById(userId) == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String error = UserForm.validate(username, email, password, false, userId, userBean);
        if (error != null) {
            // Show the form again with what the user typed
            showForm(request, response, new UserDto(userId, username, email, groups), error);
            return;
        }

        userBean.updateUser(userId, username, email, password, groups);
        response.sendRedirect(request.getContextPath() + "/Users");
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, UserDto user, String errorMessage)
            throws ServletException, IOException {
        request.setAttribute("user", user);
        request.setAttribute("errorMessage", errorMessage);
        request.setAttribute("userGroups", Roles.ALL);
        request.getRequestDispatcher("/WEB-INF/pages/users/editUser.jsp").forward(request, response);
    }
}
