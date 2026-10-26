package org.example.parkinglot.servlets;

/**
 * Reads ids from request parameters without throwing on bad input.
 */
public final class Params {

    private Params() {
    }

    /**
     * @return the id as a Long, or {@code null} if the value is missing or not a positive number
     */
    public static Long parseId(String value) {
        if (value == null) {
            return null;
        }
        try {
            long id = Long.parseLong(value.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
