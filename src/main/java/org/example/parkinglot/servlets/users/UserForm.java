package org.example.parkinglot.servlets.users;

import org.example.parkinglot.ejb.UserBean;

import java.util.regex.Pattern;

/**
 * Server-side checks for the add and edit user forms, so bad input shows a message instead of a 500 error.
 */
final class UserForm {
    static final int MAX_LENGTH = 100;
    static final int MIN_PASSWORD_LENGTH = 8;

    // A simple shape check; the browser and the @Email constraint do the detailed one
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private UserForm() {
    }

    /**
     * @param exceptUserId the user being edited, or {@code null} for a new user
     * @param passwordRequired {@code false} when editing, where an empty password keeps the old one
     * @return a message for the user, or {@code null} if the input is valid
     */
    static String validate(String username, String email, String password, boolean passwordRequired,
                           Long exceptUserId, UserBean userBean) {
        String formatError = validateFormat(username, email, password, passwordRequired);
        if (formatError != null) {
            return formatError;
        }
        if (userBean.isUsernameTaken(username, exceptUserId)) {
            return "This username is already taken.";
        }
        if (userBean.isEmailTaken(email, exceptUserId)) {
            return "Another user already has this email.";
        }
        return null;
    }

    static String validateFormat(String username, String email, String password, boolean passwordRequired) {
        if (username == null || username.isEmpty() || username.length() > MAX_LENGTH) {
            return "The username needs 1 to " + MAX_LENGTH + " characters.";
        }
        if (email == null || email.length() > MAX_LENGTH || !EMAIL.matcher(email).matches()) {
            return "Please enter a valid email address.";
        }
        boolean passwordGiven = password != null && !password.isEmpty();
        if ((passwordRequired || passwordGiven) && (!passwordGiven || password.length() < MIN_PASSWORD_LENGTH)) {
            return "The password needs at least " + MIN_PASSWORD_LENGTH + " characters.";
        }
        return null;
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
