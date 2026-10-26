package org.example.parkinglot.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import org.example.parkinglot.security.SecurityConfig;

import java.util.HashMap;
import java.util.Map;

@Stateless
public class PasswordBean {

    @Inject
    Pbkdf2PasswordHash passwordHash;

    @PostConstruct
    void init() {
        Map<String, String> parameters = new HashMap<>();
        for (String parameter : SecurityConfig.HASH_PARAMETERS) {
            String[] keyValue = parameter.split("=", 2);
            parameters.put(keyValue[0], keyValue[1]);
        }
        passwordHash.initialize(parameters);
    }

    /**
     * Returns a salted PBKDF2 hash, in the format the login identity store expects.
     */
    public String hash(String password) {
        return passwordHash.generate(password.toCharArray());
    }
}
