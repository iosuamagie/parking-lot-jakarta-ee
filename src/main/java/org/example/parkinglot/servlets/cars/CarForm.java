package org.example.parkinglot.servlets.cars;

import org.example.parkinglot.ejb.CarsBean;

/**
 * Server-side checks for the add and edit car forms, so bad input shows a message instead of a 500 error.
 */
final class CarForm {
    // Same limits as the @Size constraints on the Car entity
    static final int MIN_LENGTH = 3;
    static final int MAX_LENGTH = 100;

    private CarForm() {
    }

    /**
     * @param exceptCarId the car being edited, or {@code null} for a new car
     * @return a message for the user, or {@code null} if the input is valid
     */
    static String validate(String licensePlate, String parkingSpot, Long ownerId, Long exceptCarId, CarsBean carsBean) {
        String formatError = validateFormat(licensePlate, parkingSpot, ownerId);
        if (formatError != null) {
            return formatError;
        }
        if (carsBean.isLicensePlateTaken(licensePlate, exceptCarId)) {
            return "Another car already has this license plate.";
        }
        if (carsBean.isParkingSpotTaken(parkingSpot, exceptCarId)) {
            return "This parking spot is already taken.";
        }
        return null;
    }

    static String validateFormat(String licensePlate, String parkingSpot, Long ownerId) {
        if (!hasValidLength(licensePlate) || !hasValidLength(parkingSpot)) {
            return "The license plate and the parking spot need " + MIN_LENGTH + " to " + MAX_LENGTH + " characters.";
        }
        if (ownerId == null) {
            return "Please select an owner.";
        }
        return null;
    }

    private static boolean hasValidLength(String value) {
        return value != null && value.length() >= MIN_LENGTH && value.length() <= MAX_LENGTH;
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
