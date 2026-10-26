package org.example.parkinglot.it;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tiny browser for the tests: keeps cookies, does not follow redirects, and knows the app's forms.
 */
final class Browser {
    private static final Pattern CSRF_TOKEN = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    private final String baseUrl;
    private final CookieManager cookies = new CookieManager();
    private final HttpClient client;

    Browser(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                // Plain HTTP/1.1 like a browser on http://, not an HTTP/2 upgrade attempt
                .version(HttpClient.Version.HTTP_1_1)
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    static Browser loggedInAs(String username) {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        HttpResponse<String> response = browser.login(username, ParkPulseServer.DEMO_PASSWORD);
        if (response.statusCode() != 302 || response.headers().firstValue("Location").orElse("").contains("error")) {
            throw new IllegalStateException("Login failed for " + username);
        }
        return browser;
    }

    /**
     * Logs in like a browser: visit a protected page, send the form, follow the redirect back.
     * Jakarta Security finishes the login on that redirected request, so it must not be skipped.
     *
     * @return the answer to the login form (a redirect)
     */
    HttpResponse<String> login(String username, String password) {
        get("/Cars");
        HttpResponse<String> response = post("/j_security_check",
                Map.of("j_username", username, "j_password", password), false);
        response.headers().firstValue("Location").ifPresent(location -> send(
                HttpRequest.newBuilder(uri(URI.create(location).getRawPath() + queryOf(location))).GET().build()));
        return response;
    }

    private static String queryOf(String location) {
        String query = URI.create(location).getRawQuery();
        return query == null ? "" : "?" + query;
    }

    HttpResponse<String> get(String path) {
        return send(HttpRequest.newBuilder(uri(path)).GET().build());
    }

    /**
     * Posts a form, adding the CSRF token from the session when {@code withCsrfToken} is true.
     */
    HttpResponse<String> post(String path, Map<String, String> fields, boolean withCsrfToken) {
        Map<String, String> form = new LinkedHashMap<>(fields);
        if (withCsrfToken) {
            form.put("_csrf", csrfToken());
        }
        StringBuilder body = new StringBuilder();
        form.forEach((name, value) -> body.append(body.isEmpty() ? "" : "&")
                .append(URLEncoder.encode(name, StandardCharsets.UTF_8)).append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8)));
        return send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build());
    }

    HttpResponse<String> uploadPhoto(long carId, String fileName, byte[] content) {
        String boundary = "----parkpulse" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writePart(body, boundary, "Content-Disposition: form-data; name=\"_csrf\"\r\n\r\n" + csrfToken());
        writePart(body, boundary, "Content-Disposition: form-data; name=\"car_id\"\r\n\r\n" + carId);
        writePart(body, boundary, "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: image/png\r\n\r\n");
        body.writeBytes(content);
        body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return send(HttpRequest.newBuilder(uri("/AddCarPhoto"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build());
    }

    private static void writePart(ByteArrayOutputStream body, String boundary, String headersAndValue) {
        body.writeBytes(("--" + boundary + "\r\n" + headersAndValue).getBytes(StandardCharsets.UTF_8));
        if (!headersAndValue.endsWith("\r\n\r\n")) {
            body.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    String csrfToken() {
        // Any page with a form shows the token; the logout button is on every page for logged in users
        Matcher matcher = CSRF_TOKEN.matcher(get("/Login").body());
        if (!matcher.find()) {
            throw new IllegalStateException("No CSRF token on the page");
        }
        return matcher.group(1);
    }

    String sessionId() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("JSESSIONID"))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
