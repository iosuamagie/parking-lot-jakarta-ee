package org.example.parkinglot.common;

import java.util.List;

/**
 * The security roles of the app. Group names in the database are the same as the role names.
 */
public final class Roles {
    public static final String READ_CARS = "READ_CARS";
    public static final String WRITE_CARS = "WRITE_CARS";
    public static final String READ_USERS = "READ_USERS";
    public static final String WRITE_USERS = "WRITE_USERS";
    public static final String INVOICING = "INVOICING";

    public static final List<String> ALL = List.of(READ_CARS, WRITE_CARS, READ_USERS, WRITE_USERS, INVOICING);

    private Roles() {
    }
}
