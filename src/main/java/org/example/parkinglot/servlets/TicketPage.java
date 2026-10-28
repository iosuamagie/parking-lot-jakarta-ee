package org.example.parkinglot.servlets;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.parkinglot.common.TicketDto;
import org.example.parkinglot.ejb.ParkingSessionBean;

import java.io.IOException;
import java.util.Optional;

/** The ticket on its own page (/bilet/{code}), the one the QR code opens. Public, like the ticket itself. */
@WebServlet(name = "TicketPage", value = "/ticket/*")
public class TicketPage extends HttpServlet {

    @Inject
    ParkingSessionBean parkingSessionBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        String code = path == null ? null : path.substring(1);
        Optional<TicketDto> ticket = parkingSessionBean.findTicket(code);
        if (ticket.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // Plain strings: the page script loads the rest from /api/tickets/{code}
        request.setAttribute("ticketCode", ticket.get().ticketCode());
        request.setAttribute("ticketNumber", ticket.get().ticketNumber());
        request.getRequestDispatcher("/WEB-INF/pages/ticket.jsp").forward(request, response);
    }
}
