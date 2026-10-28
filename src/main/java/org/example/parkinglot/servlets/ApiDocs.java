package org.example.parkinglot.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** The API documentation page: Swagger UI showing the /openapi document. Public, like the docs of any API. */
@WebServlet(name = "ApiDocs", value = "/api-docs")
public class ApiDocs extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/pages/api-docs.jsp").forward(request, response);
    }
}
