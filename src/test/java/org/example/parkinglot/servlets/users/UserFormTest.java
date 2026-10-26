package org.example.parkinglot.servlets.users;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserFormTest {

    @Test
    void acceptsANewUser() {
        assertNull(UserForm.validateFormat("ana", "ana@example.com", "long-enough", true));
    }

    @Test
    void rejectsBadEmails() {
        assertNotNull(UserForm.validateFormat("ana", "ana", "long-enough", true));
        assertNotNull(UserForm.validateFormat("ana", "ana@example", "long-enough", true));
        assertNotNull(UserForm.validateFormat("ana", null, "long-enough", true));
    }

    @Test
    void requiresAPasswordForNewUsers() {
        assertNotNull(UserForm.validateFormat("ana", "ana@example.com", "", true));
        assertNotNull(UserForm.validateFormat("ana", "ana@example.com", null, true));
    }

    @Test
    void rejectsShortPasswords() {
        assertNotNull(UserForm.validateFormat("ana", "ana@example.com", "short", true));
        assertNotNull(UserForm.validateFormat("ana", "ana@example.com", "short", false));
    }

    @Test
    void anEmptyPasswordKeepsTheOldOneWhenEditing() {
        assertNull(UserForm.validateFormat("ana", "ana@example.com", "", false));
        assertNull(UserForm.validateFormat("ana", "ana@example.com", null, false));
    }

    @Test
    void rejectsAMissingUsername() {
        assertNotNull(UserForm.validateFormat("", "ana@example.com", "long-enough", true));
        assertNotNull(UserForm.validateFormat(null, "ana@example.com", "long-enough", true));
    }
}
