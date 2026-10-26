package org.example.parkinglot.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.LoginToContinue;
import jakarta.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import org.example.parkinglot.ejb.DatabaseConfig;

/**
 * Login setup with Jakarta Security. It lives in the code, so the server needs no realm configuration.
 * Passwords are checked against PBKDF2 hashes with a random salt, made by {@link org.example.parkinglot.ejb.PasswordBean}.
 */
@FormAuthenticationMechanismDefinition(
        loginToContinue = @LoginToContinue(loginPage = "/Login", errorPage = "/Login?error=true"))
@DatabaseIdentityStoreDefinition(
        dataSourceLookup = DatabaseConfig.DATA_SOURCE,
        callerQuery = "SELECT password FROM users WHERE username = ?",
        groupsQuery = "SELECT usergroup FROM usergroups WHERE username = ?",
        hashAlgorithm = Pbkdf2PasswordHash.class,
        hashAlgorithmParameters = {
                SecurityConfig.HASH_ALGORITHM,
                SecurityConfig.HASH_ITERATIONS,
                SecurityConfig.HASH_SALT_SIZE,
                SecurityConfig.HASH_KEY_SIZE
        })
@ApplicationScoped
public class SecurityConfig {
    // OWASP recommends at least 210 000 iterations for PBKDF2-HMAC-SHA512
    static final String HASH_ALGORITHM = "Pbkdf2PasswordHash.Algorithm=PBKDF2WithHmacSHA512";
    static final String HASH_ITERATIONS = "Pbkdf2PasswordHash.Iterations=210000";
    static final String HASH_SALT_SIZE = "Pbkdf2PasswordHash.SaltSizeBytes=32";
    static final String HASH_KEY_SIZE = "Pbkdf2PasswordHash.KeySizeBytes=64";

    public static final String[] HASH_PARAMETERS = {HASH_ALGORITHM, HASH_ITERATIONS, HASH_SALT_SIZE, HASH_KEY_SIZE};
}
