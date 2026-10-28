package org.example.parkinglot.servlets;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Shows the login form. The form posts to j_security_check, where Jakarta Security checks the password.
 * After a failed login the security mechanism comes back here with {@code error=true}.
 */
@WebServlet(name = "Login", value = "/Login")
public class Login extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean shownByTheLoginMechanism = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) != null;
        boolean afterAFailedLogin = request.getParameter("error") != null;
        if (!shownByTheLoginMechanism && !afterAFailedLogin) {
            // Opened directly: the login must start from a protected page, or after a good password
            // Jakarta Security has no page to return to and the browser ends up on j_security_check (404)
            response.sendRedirect(request.getContextPath() + "/Account");
            return;
        }
        if (afterAFailedLogin) {
            request.setAttribute("message", "Username or password incorrect");
        }
        request.getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // The error page can be reached with the method of the failed login POST
        doGet(request, response);
    }
}
