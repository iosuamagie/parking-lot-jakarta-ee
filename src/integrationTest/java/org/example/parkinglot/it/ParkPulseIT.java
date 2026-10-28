package org.example.parkinglot.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end checks against the real app (Payara + PostgreSQL in Docker), with the demo users.
 */
class ParkPulseIT {
    private static final Pattern PHOTO_LINK = Pattern.compile("AddCarPhoto\\?id=(\\d+)");
    private static final Pattern OWNER_OPTION = Pattern.compile("<option value=\"(\\d+)\"");
    private static final Pattern PARKED_TICKET_CODE = Pattern.compile("\"cars\":\\[\\{[^}]*?\"ticketCode\":\"([A-Za-z0-9_-]{22})\"");
    private static final Pattern EXITED_TICKET_CODE = Pattern.compile("\"ticketCode\":\"([A-Za-z0-9_-]{22})\",\"type\":\"exited\"");
    private static final Pattern FIRST_ACTIVITY_TIME = Pattern.compile("\"activity\":\\[\\{[^}]*?\"at\":\"([^\"]+)\"");

    @BeforeAll
    static void startTheApp() {
        ParkPulseServer.baseUrl();
    }

    // --- Login and roles ---

    @Test
    void anonymousVisitorsSeeTheLoginForm() {
        HttpResponse<String> page = new Browser(ParkPulseServer.baseUrl()).get("/Cars");
        assertTrue(page.body().contains("Sign in"));
        assertFalse(page.body().contains("SB-01-PKP"));
    }

    @Test
    void theApiNeedsALogin() {
        HttpResponse<String> response = new Browser(ParkPulseServer.baseUrl()).get("/api/cars");
        assertFalse(response.body().contains("licensePlate"));
    }

    @Test
    void signingInFromTheLoginButtonLandsOnAStaffPage() {
        // Like a visitor: open the login page first, not a protected page
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        HttpResponse<String> loginPage = browser.get("/Login");
        assertEquals(302, loginPage.statusCode());
        assertTrue(loginPage.headers().firstValue("Location").orElse("").endsWith("/Account"));
        assertTrue(browser.get("/Account").body().contains("Sign in"));

        // A wrong password first: the error page must still lead to a working login
        HttpResponse<String> wrong = browser.post("/j_security_check",
                Map.of("j_username", "billing", "j_password", "wrong-password"), false);
        String errorPage = URI.create(wrong.headers().firstValue("Location").orElseThrow()).getRawPath() + "?error=true";
        assertTrue(browser.get(errorPage).body().contains("Username or password incorrect"));

        HttpResponse<String> signIn = browser.post("/j_security_check",
                Map.of("j_username", "billing", "j_password", ParkPulseServer.DEMO_PASSWORD), false);
        assertEquals(302, signIn.statusCode());
        HttpResponse<String> account = browser.get(URI.create(signIn.headers().firstValue("Location").orElseThrow()).getRawPath());
        assertEquals(302, account.statusCode());
        assertTrue(account.headers().firstValue("Location").orElse("").endsWith("/billing"));
        assertEquals(200, browser.get("/billing").statusCode());
    }

    @Test
    void aWrongPasswordShowsAnError() {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        HttpResponse<String> response = browser.login("admin", "not-the-password");
        String location = response.headers().firstValue("Location").orElse("");
        assertTrue(location.endsWith("/Login?error=true"), location);
        assertTrue(browser.get("/Login?error=true").body().contains("Username or password incorrect"));
    }

    @Test
    void adminCanManageCarsAndUsers() {
        Browser admin = Browser.loggedInAs("admin");
        String cars = admin.get("/Cars").body();
        assertTrue(cars.contains("Add Car"));
        assertTrue(cars.contains("SB-01-PKP"));
        assertTrue(admin.get("/Users").body().contains("Add User"));
    }

    @Test
    void viewerCanReadButNotChangeAnything() {
        Browser viewer = Browser.loggedInAs("viewer");
        String cars = viewer.get("/Cars").body();
        assertTrue(cars.contains("SB-01-PKP"));
        assertFalse(cars.contains("Add Car"));
        assertEquals(403, viewer.get("/AddCar").statusCode());
        assertEquals(403, viewer.get("/EditUser?id=1").statusCode());
        // The old invoice form is gone, so nobody can POST to the user list any more
        assertEquals(405, viewer.post("/Users", Map.of(), true).statusCode());
    }

    // --- Billing ---

    @Test
    void billingSeesTheBillingPage() {
        String page = Browser.loggedInAs("billing").get("/billing").body();
        assertTrue(page.contains("Raport PDF pe perioadă"), page);
        assertTrue(page.contains("PP-"), page);
    }

    @Test
    void viewerCannotSeeBilling() {
        Browser viewer = Browser.loggedInAs("viewer");
        assertEquals(403, viewer.get("/billing").statusCode());
        assertEquals(403, viewer.get("/api/billing/tickets").statusCode());
    }

    @Test
    void badBillingDatesGiveBadRequest() {
        Browser billing = Browser.loggedInAs("billing");
        assertEquals(400, billing.get("/api/billing/tickets?from=ieri&to=2026-10-27").statusCode());
        assertEquals(400, billing.get("/api/billing/tickets?from=2026-10-27&to=2026-10-01").statusCode());
        assertEquals(400, billing.get("/api/billing/report.pdf?from=2020-01-01&to=2026-10-27").statusCode());
        assertEquals(400, billing.get("/billing?period=custom&from=ieri&to=azi").statusCode());
    }

    @Test
    void theBillingApiListsPaidTickets() {
        HttpResponse<String> response = Browser.loggedInAs("billing").get("/api/billing/tickets");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"items\":[{"), response.body());
        assertFalse(response.body().contains("\"total\":0,"), response.body());
    }

    @Test
    void theReportIsAPdf() {
        HttpResponse<byte[]> response = Browser.loggedInAs("billing").getBytes("/api/billing/report.pdf");
        assertEquals(200, response.statusCode());
        assertEquals("%PDF", new String(response.body(), 0, 4, StandardCharsets.US_ASCII));
    }

    // --- Sessions and CSRF ---

    @Test
    void theSessionIdChangesAfterLogin() {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        browser.get("/Cars");
        String before = browser.sessionId();
        browser.login("admin", ParkPulseServer.DEMO_PASSWORD);
        browser.get("/Cars");
        assertNotEquals(before, browser.sessionId());
    }

    @Test
    void theSessionCookieIsHttpOnlyAndSameSite() {
        HttpResponse<String> response = new Browser(ParkPulseServer.baseUrl()).get("/Login");
        String cookie = response.headers().firstValue("Set-Cookie").orElse("");
        assertTrue(cookie.contains("HttpOnly"), cookie);
        assertTrue(cookie.toLowerCase().contains("samesite=lax"), cookie);
    }

    @Test
    void formsWithoutTheCsrfTokenAreRejected() {
        Browser admin = Browser.loggedInAs("admin");
        HttpResponse<String> response = admin.post("/AddCar",
                Map.of("license_plate", "NO-CSRF-1", "parking_spot", "Z01", "owner_id", "1"), false);
        assertEquals(403, response.statusCode());
    }

    // --- Input handling ---

    @Test
    void userTextIsShownAsTextNotRunAsCode() {
        Browser admin = Browser.loggedInAs("admin");
        String plate = "<script>alert('" + shortId() + "')</script>";
        HttpResponse<String> response = addCar(admin, plate, "X-" + shortId());
        assertEquals(302, response.statusCode(), response.body());

        String cars = admin.get("/Cars").body();
        assertTrue(cars.contains("&lt;script&gt;"));
        assertFalse(cars.contains(plate));
    }

    @Test
    void aTakenParkingSpotShowsAMessage() {
        Browser admin = Browser.loggedInAs("admin");
        HttpResponse<String> response = addCar(admin, "DUP-" + shortId(), "A01"); // A01 belongs to a demo car
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("This parking spot is already taken."));
    }

    @Test
    void badIdsGiveNotFoundInsteadOfAServerError() {
        Browser admin = Browser.loggedInAs("admin");
        assertEquals(404, admin.get("/EditCar?id=abc").statusCode());
        assertEquals(404, admin.get("/EditCar?id=999999").statusCode());
        assertEquals(404, admin.get("/EditUser?id=-1").statusCode());
        assertEquals(404, admin.get("/api/cars/999999").statusCode());
    }

    @Test
    void theApiReturnsCarsAsJson() {
        HttpResponse<String> response = Browser.loggedInAs("viewer").get("/api/cars");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertTrue(response.body().contains("\"licensePlate\":\"SB-01-PKP\""));
    }

    @Test
    void theCarsApiStillNeedsALogin() {
        assertFalse(new Browser(ParkPulseServer.baseUrl()).get("/api/cars").body().contains("licensePlate"));
    }

    // --- The public live lot ---

    @Test
    void theLotIsPublicAndHasNoSessionCookie() {
        HttpResponse<String> response = new Browser(ParkPulseServer.baseUrl()).get("/api/lot");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"clockSpeed\":60"));
        assertTrue(response.body().contains("\"A01\""));
        assertTrue(response.headers().allValues("Set-Cookie").stream().noneMatch(c -> c.startsWith("JSESSIONID")));
    }

    @Test
    void theLiveFeedStartsWithASnapshot() {
        String first = new Browser(ParkPulseServer.baseUrl()).readFirstSseEvent("/api/events", Duration.ofSeconds(10));
        assertTrue(first.contains("event: snapshot"), first);
        assertTrue(first.contains("\"cars\""), first);
    }

    @Test
    void theSimulatorParksCars() throws InterruptedException {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        Instant first = latestActivity(browser);
        Instant deadline = Instant.now().plusSeconds(30);
        while (!latestActivity(browser).isAfter(first)) {
            assertTrue(Instant.now().isBefore(deadline), "No car moved in 30 seconds");
            Thread.sleep(2000);
        }
    }

    private static Instant latestActivity(Browser browser) {
        Matcher matcher = FIRST_ACTIVITY_TIME.matcher(browser.get("/api/lot").body());
        assertTrue(matcher.find(), "The lot has no activity");
        return Instant.parse(matcher.group(1));
    }

    @Test
    void theHomePageIsTheLiveLotForEveryone() {
        String body = new Browser(ParkPulseServer.baseUrl()).get("/").body();
        assertTrue(body.contains("id=\"lot\""));
        assertTrue(body.contains("Autentificare"));
    }

    // --- Tickets ---

    private static String aParkedTicketCode(Browser browser) {
        String lot = browser.get("/api/lot").body();
        Matcher matcher = PARKED_TICKET_CODE.matcher(lot);
        assertTrue(matcher.find(), "No car is parked: " + lot);
        return matcher.group(1);
    }

    @Test
    void aTicketIsPublicByItsCode() {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        String code = aParkedTicketCode(browser);
        HttpResponse<String> response = browser.get("/api/tickets/" + code);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"ticketNumber\":\"PP-"));
        assertTrue(response.body().contains("\"plate\":\""));
    }

    @Test
    void aBadTicketCodeIsNotFound() {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        assertEquals(404, browser.get("/api/tickets/abc").statusCode());
        assertEquals(404, browser.get("/api/tickets/" + "x".repeat(500)).statusCode());
        assertEquals(404, browser.get("/api/tickets/AAAAAAAAAAAAAAAAAAAAAA").statusCode());
        assertEquals(404, browser.get("/api/tickets/abc/qr.svg").statusCode());
        assertEquals(404, browser.get("/bilet/AAAAAAAAAAAAAAAAAAAAAA").statusCode());
        assertEquals(404, browser.get("/bilet/").statusCode());
        // Payara itself turns down encoded slashes, before the request reaches the app
        assertEquals(400, browser.get("/bilet/..%2F..%2Fetc").statusCode());
    }

    @Test
    void theQrCodeIsAnSvg() {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        HttpResponse<String> response = browser.get("/api/tickets/" + aParkedTicketCode(browser) + "/qr.svg");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("image/svg+xml"));
        assertTrue(response.body().contains("qr-row"));
    }

    @Test
    void theTicketPageOpensForEveryone() {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        String code = aParkedTicketCode(browser);
        HttpResponse<String> response = browser.get("/bilet/" + code);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("data-ticket-code=\"" + code + "\""));
    }

    @Test
    void aReceiptIsAPdfForAFinishedTicket() {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        Matcher matcher = EXITED_TICKET_CODE.matcher(browser.get("/api/lot").body());
        assertTrue(matcher.find(), "No car has left yet");
        HttpResponse<byte[]> response = browser.getBytes("/api/tickets/" + matcher.group(1) + "/receipt.pdf");
        assertEquals(200, response.statusCode());
        assertEquals("application/pdf", response.headers().firstValue("Content-Type").orElse(""));
        assertTrue(response.headers().firstValue("Content-Disposition").orElse("").contains("chitanta-PP-"));
        assertEquals("%PDF", new String(response.body(), 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void aReceiptForAParkedCarIsAConflict() {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        HttpResponse<String> response = browser.get("/api/tickets/" + aParkedTicketCode(browser) + "/receipt.pdf");
        assertEquals(409, response.statusCode());
        assertTrue(response.body().contains("Chitanța apare după ieșire"));
    }

    // --- API docs ---

    @Test
    void theOpenApiDocumentListsTheRoutes() {
        String doc = new Browser(ParkPulseServer.baseUrl()).get("/openapi?format=json").body();
        for (String path : List.of("/api/lot", "/api/events", "/api/tickets/{code}", "/api/tickets/{code}/receipt.pdf",
                "/api/billing/tickets", "/api/cars")) {
            assertTrue(doc.contains("\"" + path + "\""), path);
        }
        assertTrue(doc.contains("ParkPulse API"), doc);
        // "Try it" must call the address the page was opened with, not the container's own name
        assertTrue(doc.contains("\"servers\":[{\"url\":\"/\""), doc);
    }

    @Test
    void theApiDocsPageLoadsSwaggerUi() {
        Browser browser = new Browser(ParkPulseServer.baseUrl());
        assertTrue(browser.get("/api-docs").body().contains("swagger-ui-bundle.js"));
        assertEquals(200, browser.get("/webjars/swagger-ui/5.33.2/swagger-ui-bundle.js").statusCode());
    }

    // --- Photo uploads ---

    @Test
    void aRealImageIsAcceptedAndServedSafely() {
        Browser admin = Browser.loggedInAs("admin");
        long carId = firstCarId(admin);

        assertEquals(302, admin.uploadPhoto(carId, "car.png", tinyPng()).statusCode());

        HttpResponse<String> photo = admin.get("/CarPhotos?id=" + carId);
        assertEquals(200, photo.statusCode());
        assertEquals("image/png", photo.headers().firstValue("Content-Type").orElse(""));
        assertEquals("nosniff", photo.headers().firstValue("X-Content-Type-Options").orElse(""));
    }

    @Test
    void htmlPretendingToBeAnImageIsRejected() {
        Browser admin = Browser.loggedInAs("admin");
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        assertEquals(415, admin.uploadPhoto(firstCarId(admin), "fake.png", html).statusCode());
    }

    @Test
    void photosOverTwoMegabytesAreRejected() {
        Browser admin = Browser.loggedInAs("admin");
        byte[] big = new byte[3 * 1024 * 1024];
        System.arraycopy(tinyPng(), 0, big, 0, 8); // a PNG signature, so only the size is wrong
        assertEquals(413, admin.uploadPhoto(firstCarId(admin), "big.png", big).statusCode());
    }

    // --- Helpers ---

    private static HttpResponse<String> addCar(Browser browser, String licensePlate, String parkingSpot) {
        Matcher owner = OWNER_OPTION.matcher(browser.get("/AddCar").body());
        assertTrue(owner.find(), "no owners to choose from");
        return browser.post("/AddCar",
                Map.of("license_plate", licensePlate, "parking_spot", parkingSpot, "owner_id", owner.group(1)), true);
    }

    private static long firstCarId(Browser browser) {
        Matcher link = PHOTO_LINK.matcher(browser.get("/Cars").body());
        assertTrue(link.find(), "no cars on the page");
        return Long.parseLong(link.group(1));
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * A valid 1x1 red PNG.
     */
    private static byte[] tinyPng() {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        png.writeBytes(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        writeChunk(png, "IHDR", new byte[]{0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0});
        Deflater deflater = new Deflater();
        deflater.setInput(new byte[]{0, (byte) 0xFF, 0, 0});
        deflater.finish();
        byte[] compressed = new byte[64];
        int length = deflater.deflate(compressed);
        byte[] data = new byte[length];
        System.arraycopy(compressed, 0, data, 0, length);
        writeChunk(png, "IDAT", data);
        writeChunk(png, "IEND", new byte[0]);
        return png.toByteArray();
    }

    private static void writeChunk(ByteArrayOutputStream png, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        writeInt(png, data.length);
        png.writeBytes(typeBytes);
        png.writeBytes(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        writeInt(png, (int) crc.getValue());
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.writeBytes(new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value});
    }
}
