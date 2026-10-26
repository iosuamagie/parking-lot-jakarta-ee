package org.example.parkinglot.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Protects every POST form against CSRF: the form must send back the random token kept in the session.
 * Forms add the token with the {@code <t:csrf/>} tag.
 */
@WebFilter(urlPatterns = "/*", dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class CsrfFilter extends HttpFilter {
    public static final String TOKEN_NAME = "csrfToken";
    public static final String PARAMETER_NAME = "_csrf";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String expected = tokenFor(request.getSession());

        // Forwards (for example to the login page) only need the token to exist, there is nothing to check
        if (request.getDispatcherType() == DispatcherType.REQUEST
                && "POST".equals(request.getMethod())
                && !request.getRequestURI().endsWith("/j_security_check")
                && !matches(expected, request.getParameter(PARAMETER_NAME))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
            return;
        }
        chain.doFilter(request, response);
    }

    static String tokenFor(HttpSession session) {
        String token = (String) session.getAttribute(TOKEN_NAME);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(TOKEN_NAME, token);
        }
        return token;
    }

    static boolean matches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        // Constant-time comparison, so the token cannot be guessed from response times
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
