package org.example.parkinglot.parking;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Ticket codes are 16 random bytes in base64url (22 characters), so nobody can guess another person's ticket.
 * The short number printed on the ticket (PP-00042) is only for people to read.
 */
public final class TicketCodes {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_-]{22}");

    private TicketCodes() {
    }

    public static String newCode() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static boolean looksValid(String code) {
        return code != null && CODE.matcher(code).matches();
    }

    public static String displayNumber(long id) {
        return String.format(Locale.ROOT, "PP-%05d", id);
    }
}
