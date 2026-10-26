package org.example.parkinglot.servlets.cars;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.example.parkinglot.common.CarDto;
import org.example.parkinglot.common.ImageTypes;
import org.example.parkinglot.common.Roles;
import org.example.parkinglot.ejb.CarsBean;
import org.example.parkinglot.servlets.Params;

import java.io.IOException;
import java.io.InputStream;

@ServletSecurity(value = @HttpConstraint(rolesAllowed = {Roles.WRITE_CARS}))
@MultipartConfig(maxFileSize = AddCarPhoto.MAX_PHOTO_BYTES, maxRequestSize = AddCarPhoto.MAX_PHOTO_BYTES + 64 * 1024)
@WebServlet(name = "AddCarPhoto", value = "/AddCarPhoto")
public class AddCarPhoto extends HttpServlet {
    static final long MAX_PHOTO_BYTES = 2 * 1024 * 1024;

    @Inject
    CarsBean carsBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long carId = Params.parseId(request.getParameter("id"));
        CarDto car = carId == null ? null : carsBean.findById(carId);
        if (car == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("car", car);
        request.getRequestDispatcher("/WEB-INF/pages/cars/addCarPhoto.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long carId = Params.parseId(request.getParameter("car_id"));
        Part filePart;
        try {
            filePart = request.getPart("file");
        } catch (RuntimeException tooLarge) {
            // IllegalStateException in the Servlet spec, a plain RuntimeException on Payara
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "The photo can be at most 2 MB");
            return;
        }
        if (carId == null || filePart == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        byte[] fileContent;
        try (InputStream input = filePart.getInputStream()) {
            fileContent = input.readAllBytes();
        }

        // The browser's content type can be faked, so check the file's own bytes
        String fileType = ImageTypes.detect(fileContent);
        if (fileType == null) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Only PNG, JPEG, GIF or WebP images are allowed");
            return;
        }

        if (!carsBean.addPhotoToCar(carId, baseName(filePart.getSubmittedFileName()), fileType, fileContent)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/Cars");
    }

    // Some browsers send the full path ("C:\photos\car.png"); keep only the file name
    static String baseName(String submittedFileName) {
        if (submittedFileName == null) {
            return "photo";
        }
        int lastSeparator = Math.max(submittedFileName.lastIndexOf('/'), submittedFileName.lastIndexOf('\\'));
        String name = submittedFileName.substring(lastSeparator + 1);
        return name.length() > 255 ? name.substring(0, 255) : name;
    }
}
