package org.example.parkinglot.servlets.users;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.HttpMethodConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.parkinglot.common.Roles;
import org.example.parkinglot.common.UserDto;
import org.example.parkinglot.ejb.InvoiceBean;
import org.example.parkinglot.ejb.UserBean;
import org.example.parkinglot.servlets.Params;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ServletSecurity(
        value = @HttpConstraint(rolesAllowed = {Roles.READ_USERS}),
        httpMethodConstraints = {@HttpMethodConstraint(value = "POST", rolesAllowed = {Roles.INVOICING})}
)
@WebServlet(name = "Users", value = "/Users")
public class Users extends HttpServlet {
    @Inject
    UserBean userBean;

    @Inject
    InvoiceBean invoiceBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<UserDto> users = userBean.findAllUsers();
        request.setAttribute("users", users);

        if (request.isUserInRole(Roles.INVOICING)) {
            // The ids picked before, so their checkboxes stay checked
            request.setAttribute("activeUserIds", invoiceBean.getUserIds());

            if (!invoiceBean.getUserIds().isEmpty()) {
                request.setAttribute("invoices", userBean.findUsernamesByUserIds(invoiceBean.getUserIds()));
            }
        }

        request.getRequestDispatcher("/WEB-INF/pages/users/users.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String[] userIdsAsString = request.getParameterValues("user_ids");

        // No checked boxes means an empty selection, which clears the invoice
        Set<Long> userIds = new HashSet<>();
        if (userIdsAsString != null) {
            for (String userIdAsString : userIdsAsString) {
                Long userId = Params.parseId(userIdAsString);
                if (userId != null) {
                    userIds.add(userId);
                }
            }
        }
        invoiceBean.setUserIds(userIds);

        response.sendRedirect(request.getContextPath() + "/Users");
    }
}
