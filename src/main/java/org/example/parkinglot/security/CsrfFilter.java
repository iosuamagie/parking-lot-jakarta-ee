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
 * Forms add the token with the {@code <t:csrf/>} tag. It also renews the session id after login.
 */
@WebFilter(urlPatterns = "/*", dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class CsrfFilter extends HttpFilter {
    public static final String TOKEN_NAME = "csrfToken";
    public static final String PARAMETER_NAME = "_csrf";
    private static final String LOGGED_IN_AS = "loggedInAs";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        renewSessionAfterLogin(request);
        String expected = tokenFor(request.getSession());

        // Forwards (for example to the login page) only need the token to exist, there is nothing to check
        if (request.getDispatcherType() == DispatcherType.REQUEST
                && "POST".equals(request.getMethod())
                && !request.getRequestURI().endsWith("/j_security_check")) {
            String actual;
            try {
                actual = request.getParameter(PARAMETER_NAME);
            } catch (RuntimeException unreadable) {
                // Uploads over the @MultipartConfig limit can't be read (IllegalStateException in the
                // Servlet spec, a plain RuntimeException on Payara)
                boolean upload = String.valueOf(request.getContentType()).startsWith("multipart/");
                response.sendError(upload ? HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE : HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (!matches(expected, actual)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Gives the session a new id (and a new CSRF token) on the first request after a login,
     * so a session id planted before the login is useless afterwards (session fixation).
     */
    private static void renewSessionAfterLogin(HttpServletRequest request) {
        String user = request.getRemoteUser();
        HttpSession session = request.getSession(false);
        if (user == null || session == null || user.equals(session.getAttribute(LOGGED_IN_AS))) {
            return;
        }
        request.changeSessionId();
        session.setAttribute(LOGGED_IN_AS, user);
        session.removeAttribute(TOKEN_NAME);
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
