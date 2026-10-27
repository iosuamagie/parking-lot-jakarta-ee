package org.example.parkinglot.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
        assertTrue(admin.get("/Users").body().contains("Invoice"));
    }

    @Test
    void viewerCanReadButNotChangeAnything() {
        Browser viewer = Browser.loggedInAs("viewer");
        String cars = viewer.get("/Cars").body();
        assertTrue(cars.contains("SB-01-PKP"));
        assertFalse(cars.contains("Add Car"));
        assertEquals(403, viewer.get("/AddCar").statusCode());
        assertEquals(403, viewer.get("/EditUser?id=1").statusCode());
        assertEquals(403, viewer.post("/Users", Map.of(), true).statusCode());
    }

    @Test
    void billingCanPickUsersForAnInvoice() {
        Browser billing = Browser.loggedInAs("billing");
        assertEquals(302, billing.post("/Users", Map.of(), true).statusCode());
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
