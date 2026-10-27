# Faza 3 – Parcarea live: plan de implementare

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transformăm ParkPulse într-o parcare publică simulată, vizibilă live în pixel art, cu bilet QR, tarif, chitanță PDF, pagină de încasări și documentație de API, totul în Jakarta EE.

**Architecture:** Un ceas al parcării accelerat (×60) și un simulator EJB (`@Schedule`) creează și închid `ParkingSession`-uri în PostgreSQL. După fiecare schimbare, un hub SSE din JAX-RS trimite evenimente browserelor. Paginile sunt JSP. Harta e desenată pe Canvas 2D de module JavaScript simple (ES modules, fără build). QR-ul (ZXing) și PDF-urile (OpenPDF) se generează pe server. MicroProfile OpenAPI din Payara descrie API-ul, iar Swagger UI (webjar) îl afișează.

**Tech Stack:** Java 21, Jakarta EE 10 API pe Payara 7.2026.9, JPA (EclipseLink), JAX-RS + SSE, JSON-B, JSP/JSTL, PostgreSQL 17, Gradle, JUnit 5, Testcontainers 2.0.5, JavaScript simplu (ES modules) + Canvas 2D + CSS.

**Spec:** `docs/superpowers/specs/2026-10-27-faza-3-parcare-live-design.md`. Executorii citesc spec-ul **și** planul; schițele aprobate sunt în `.superpowers/brainstorm/845-1793093555/content/` (`live-page-v4.html` pentru pagina live și bilet, `billing-page.html` pentru încasări).

## Global Constraints

- Doar Java/Jakarta EE + JSP + JavaScript simplu + CSS. **Fără** Node, npm, React sau framework-uri de frontend și fără pas de build pentru JS.
- Dependențe noi, **exact** aceste versiuni, adăugate doar după „da”-ul lui Iosua (Task 0):
  - `com.google.zxing:core:3.5.4`
  - `com.github.librepdf:openpdf:3.0.5`
  - `com.github.librepdf:openpdf-fonts-extra:3.0.5`
  - `org.webjars:swagger-ui:5.33.2`
  - `org.eclipse.microprofile.openapi:microprofile-openapi-api:4.0.2` (`compileOnly`)
- Nicio imagine sau fișier descărcat: mașinile, barierele, marcajele, simbolul scaunului cu rotile și fontul pixel sunt desenate în cod.
- Tarif: primele 15 minute gratuit, apoi 6 lei/oră, socotit pe fiecare minut început (10 bani/minut). Sumele sunt `int`, în bani.
- Ceasul parcării: ×60 (variabila `PARKPULSE_CLOCK_SPEED`, implicit `60`). Toate orele salvate și afișate sunt pe acest ceas. Zona orară pentru „azi”/zile: `Europe/Bucharest`.
- 24 de locuri: `A01`–`A12` (rândul de sus) și `B01`–`B12` (rândul de jos). Locurile pentru persoane cu dizabilități sunt `A01`, `A02`, `A03`.
- Codul biletului: 16 octeți din `SecureRandom`, base64url fără padding (22 de caractere). Numărul afișat: `PP-` + `id` pe 5 cifre (`PP-00042`).
- `@ApplicationPath("/api")`. `/resources/*` dispare, fără alias.
- Rute publice: `/`, `/bilet/*`, `/api/lot`, `/api/events`, `/api/tickets/*`, `/api-docs`, `/openapi`, `/webjars/*`, `/scripts/*`, `/styles/*`. Cu rol: `/api/cars/*` (`READ_CARS`), `/api/billing/*` și `/billing` (`INVOICING`).
- Animații: maximum 1–2 lucruri animate în același timp; `prefers-reduced-motion: reduce` oprește traseele, FLIP-ul și „printarea” QR-ului.
- Texte vizibile în română, cu diacritice corecte (ă, â, î, ș, ț). Codul, numele de fișiere și mesajele de commit sunt în engleză.
- **Înainte de fiecare commit, Iosua vede modificările în IntelliJ și spune OK.** Commiturile sunt doar pe numele lui (`iosuamagie`), fără `Co-Authored-By` sau alte mențiuni Claude.
- Pe PC-ul lui Iosua se rulează cu `PARKPULSE_PORT=8081` (8080 e ocupat).
- O singură instanță Payara: simulatorul singleton serializează toate scrierile, deci nu folosim blocări în baza de date.

## Review Focus

1. **Un cod de bilet greșit** (`/bilet/abc`, `/api/tickets/../x`, un cod de 500 de caractere) → 404 curat, nu 500 → testul `aBadTicketCodeIsNotFound` (Task 4).
2. **Parametrii de dată greșiți la încasări** (`from=ieri`, `from` după `to`, un interval mai lung de 366 de zile) → 400 cu mesaj, nu 500 → testul `badBillingDatesGiveBadRequest` (Task 6).
3. **Chitanța cerută cât mașina e încă în parcare** → 409, nu un PDF stricat → testul `aReceiptForAParkedCarIsAConflict` (Task 5).
4. **Parcarea plină sau doar locuri pentru persoane cu dizabilități libere, iar mașina nu are permis** → simulatorul face o ieșire, nu se blochează și nu aruncă excepții → testul `aCarWithoutPermitNeverGetsAnAccessibleSpot` (Task 2).
5. **Tab-ul stă mult în fundal și apoi revine** → harta se redesenează din `snapshot`, fără sute de animații puse la coadă → verificarea manuală din Task 3, Step 7.

---

## Structura fișierelor

```
build.gradle                                   (modificat: dependențe noi)
docker-compose.yml                             (PARKPULSE_SIMULATOR, PARKPULSE_CLOCK_SPEED, PARKPULSE_PUBLIC_URL)
src/main/java/org/example/parkinglot/
  parking/            logică pură, fără EJB (testată unitar)
    Tariff.java  AcceleratedClock.java  Spots.java  TicketCodes.java
    PlateGenerator.java  CarModel.java  CarLooks.java  SimulatorPolicy.java
  entities/ParkingSession.java
  common/             DTO-uri (records)
    ParkedCarDto.java  ActivityDto.java  LotStatsDto.java  LotSnapshotDto.java  TicketDto.java
    PaidTicketDto.java  DayTotalDto.java  BillingSummaryDto.java  PageDto.java
  ejb/
    ParkingClockBean.java  ParkingSessionBean.java  SimulatorBean.java  BillingBean.java
    DemoDataBean.java (modificat)  InvoiceBean.java (șters în Task 6)
  live/LiveHub.java   hub-ul SSE (@ApplicationScoped)
  documents/QrSvg.java  ReceiptPdf.java  ReportPdf.java  PdfFonts.java
  rest/  ParkingLotApplication.java (modificat)  LotResource.java  LiveEventsResource.java
         TicketsResource.java  BillingResource.java  CarsController.java (adnotări OpenAPI)
  servlets/ TicketPage.java  Billing.java  ApiDocs.java  users/Users.java (modificat)
  security/CsrfFilter.java (modificat: sare peste /api/*)
src/main/webapp/
  index.jsp (pagina live)  WEB-INF/pages/ticket.jsp  billing.jsp  api-docs.jsp
  WEB-INF/pages/menu.jsp (bară nouă)  WEB-INF/tags/pageTemplate.tag (app.css, clase pe body)
  styles/app.css
  scripts/ tariff.js  lot-geometry.js  pixel-art.js  lot-renderer.js  live.js
           ticket-card.js  ticket-popup.js  ticket-page.js  api-docs.js
src/test/java/org/example/parkinglot/parking/*Test.java   documents/*Test.java
src/integrationTest/java/org/example/parkinglot/it/ParkPulseIT.java (+ teste noi), Browser.java (getBytes, readFirstSseEvent)
```

---

### Task 0: Aprobarea dependențelor (poartă, fără cod)

- [ ] **Step 1:** Arată-i lui Iosua lista din Global Constraints, cu licența (ZXing: Apache-2.0; OpenPDF și fonts-extra: LGPL-2.1/MPL-2.0; Swagger UI webjar: Apache-2.0; MicroProfile OpenAPI API: Apache-2.0), data versiunii și cine o întreține. Fiecare e proiectul original: `zxing/zxing`, `LibrePDF/OpenPDF`, `webjars/swagger-ui`, `eclipse/microprofile-open-api`.
- [ ] **Step 2:** Așteaptă „da”. Fără el, nu se trece la Task 4 (ZXing), Task 5 (OpenPDF) și Task 7 (Swagger/MP OpenAPI). Task-urile 1–3 nu au nevoie de dependențe noi.

---

### Task 1: Sesiuni de parcare, tarif și ceasul parcării

**Files:**
- Create: `parking/Tariff.java`, `parking/AcceleratedClock.java`, `parking/Spots.java`, `parking/TicketCodes.java`, `parking/CarModel.java`, `entities/ParkingSession.java`, `ejb/ParkingClockBean.java`, `ejb/ParkingSessionBean.java`, DTO-urile `ParkedCarDto`, `ActivityDto`, `LotStatsDto`, `TicketDto`
- Modify: `src/main/resources/META-INF/persistence.xml` (adaugă `<class>…ParkingSession</class>`)
- Test: `src/test/java/org/example/parkinglot/parking/TariffTest.java`, `AcceleratedClockTest.java`, `SpotsTest.java`, `TicketCodesTest.java`

**Interfaces:**
- Produces:
  - `final class Tariff { static final int FREE_MINUTES = 15; static final int BANI_PER_MINUTE = 10; static int amountBani(Duration stay); static String format(int bani); }`. `format` scrie `"10,30 lei"`.
  - `final class AcceleratedClock { AcceleratedClock(Instant parkingStart, Instant realStart, int speed, Supplier<Instant> realNow); Instant now(); int speed(); static final ZoneId ZONE = ZoneId.of("Europe/Bucharest"); }`
  - `final class Spots { static final List<String> ALL; static final Set<String> ACCESSIBLE; static final int CAPACITY = 24; static boolean isValid(String spot); }`
  - `final class TicketCodes { static String newCode(); static boolean looksValid(String code); static String displayNumber(long id); }`
  - `enum CarModel { MINI, SEDAN, SUV, TAXI, VAN }`
  - `@Entity @Table(name="parking_session") class ParkingSession` cu: `Long id` (secvență), `String ticketCode` (unic, 22), `String licensePlate` (20), `String spot` (3), `CarModel carModel` (`@Enumerated(STRING)`), `String carColor` (7, `#rrggbb`), `boolean accessiblePermit`, `Instant enteredAt`, `Instant exitedAt` (nullable), `Integer amountBani` (nullable), plus getters/setters.
  - `@Singleton @Startup class ParkingClockBean { Instant now(); int speed(); }`
  - `@Stateless class ParkingSessionBean`:
    - `ParkedCarDto enter(String plate, String spot, CarModel model, String color, boolean permit)`
    - `ActivityDto exit(String spot)` (închide sesiunea deschisă de pe loc)
    - `List<ParkedCarDto> findParked()`, `Optional<TicketDto> findTicket(String code)`
    - `LotStatsDto stats()`, `List<ActivityDto> recentActivity(int limit)`
  - Records:
    - `ParkedCarDto(String spot, String plate, String ticketCode, String ticketNumber, CarModel model, String color, boolean accessiblePermit, Instant enteredAt)`
    - `ActivityDto(String type /* "entered" | "exited" */, String plate, String spot, String ticketCode, Integer amountBani, Instant at)`
    - `LotStatsDto(int capacity, int free, int revenueTodayBani, int revenueYesterdaySameTimeBani, int carsToday, long averageStayMinutes)`
    - `TicketDto(String ticketCode, String ticketNumber, String plate, String spot, CarModel model, String color, boolean accessiblePermit, Instant enteredAt, Instant exitedAt, Integer amountBani, Instant clockNow, int clockSpeed)`

- [ ] **Step 1: Scrie testele care pică**

```java
// TariffTest
@Test void theFirstFifteenMinutesAreFree() { assertEquals(0, Tariff.amountBani(Duration.ofMinutes(15))); }
@Test void everyStartedMinuteAfterThatCostsTenBani() {
    assertEquals(10, Tariff.amountBani(Duration.ofMinutes(15).plusSeconds(1)));
    assertEquals(10, Tariff.amountBani(Duration.ofMinutes(16)));
    assertEquals(20, Tariff.amountBani(Duration.ofMinutes(16).plusSeconds(1)));
    assertEquals(1030, Tariff.amountBani(Duration.ofMinutes(118)));   // ca în schiță: 10,30 lei
    assertEquals(0, Tariff.amountBani(Duration.ZERO));
}
@Test void amountsAreShownInLei() { assertEquals("10,30 lei", Tariff.format(1030)); assertEquals("0,00 lei", Tariff.format(0)); }

// AcceleratedClockTest – ceas real controlat de test
@Test void oneRealSecondIsOneParkingMinute() {
    Instant real0 = Instant.parse("2026-10-27T10:00:00Z"), park0 = Instant.parse("2026-01-01T08:00:00Z");
    AtomicReference<Instant> real = new AtomicReference<>(real0);
    AcceleratedClock clock = new AcceleratedClock(park0, real0, 60, real::get);
    real.set(real0.plusSeconds(90));
    assertEquals(park0.plus(Duration.ofMinutes(90)), clock.now());
}

// SpotsTest
@Test void thereAre24SpotsAndThreeAccessible() {
    assertEquals(24, Spots.ALL.size()); assertEquals("A01", Spots.ALL.get(0)); assertEquals("B12", Spots.ALL.get(23));
    assertEquals(Set.of("A01", "A02", "A03"), Spots.ACCESSIBLE);
    assertFalse(Spots.isValid("C01")); assertFalse(Spots.isValid(null));
}

// TicketCodesTest
@Test void codesAre22UrlSafeCharactersAndUnique() {
    String a = TicketCodes.newCode(), b = TicketCodes.newCode();
    assertTrue(a.matches("[A-Za-z0-9_-]{22}")); assertNotEquals(a, b);
    assertTrue(TicketCodes.looksValid(a)); assertFalse(TicketCodes.looksValid("../etc")); assertFalse(TicketCodes.looksValid("x".repeat(500)));
}
@Test void displayNumbersHaveFiveDigits() { assertEquals("PP-00042", TicketCodes.displayNumber(42)); }
```

- [ ] **Step 2:** Rulează `./gradlew test`. Expected: FAIL, pentru că clasele nu există.
- [ ] **Step 3:** Implementează `Tariff`, `AcceleratedClock`, `Spots`, `TicketCodes`, `CarModel` după semnăturile de mai sus.
  - `AcceleratedClock.now() = parkingStart + (realNow − realStart) × speed`.
- [ ] **Step 4:** Rulează `./gradlew test`. Expected: PASS, toate testele, inclusiv cele vechi.
- [ ] **Step 5:** Implementează `ParkingSession` și `ParkingClockBean`.
  - În `@PostConstruct`, `ParkingClockBean` citește `MAX(enteredAt)` și `MAX(exitedAt)` din `ParkingSession`. Ancora e cea mai mare dintre ele, iar dacă baza e goală, `Instant.now()`.
  - Viteza vine din `PARKPULSE_CLOCK_SPEED` (implicit 60; o valoare invalidă sau sub 1 → 60, cu warning în log).
- [ ] **Step 6:** Implementează `ParkingSessionBean`.
  - `enter`: refuză cu `IllegalStateException` dacă locul e invalid, locul e ocupat sau plăcuța e deja în parcare.
  - `exit`: calculează `amountBani` cu `Tariff` din `enteredAt` → `clock.now()`.
  - `stats`:
    - „azi” = ziua `clock.now()` în `AcceleratedClock.ZONE`;
    - `revenueYesterdaySameTimeBani` = încasările de ieri până la aceeași oră;
    - `averageStayMinutes` = media sesiunilor închise azi (0 dacă nu sunt).
  - `recentActivity(limit)`: ultimele `limit` intrări și ultimele `limit` ieșiri, unite și sortate descrescător după `at`, din care se păstrează primele `limit`.
- [ ] **Step 7:** Rulează `./gradlew war`. Expected: BUILD SUCCESSFUL. (Persistența lui `Instant` se verifică în Task 2, în testul de integrare. Dacă EclipseLink refuză `Instant`, se trece la `java.time.LocalDateTime` în UTC în entitate, iar DTO-urile rămân pe `Instant`.)
- [ ] **Step 8:** Iosua verifică în IntelliJ, apoi commit:

```bash
git add src/main/java src/main/resources src/test/java
git commit -m "Add parking sessions, the tariff and the accelerated parking clock"
```

---

### Task 2: Simulatorul și datele demo

**Files:**
- Create: `parking/PlateGenerator.java`, `parking/CarLooks.java`, `parking/SimulatorPolicy.java`, `ejb/SimulatorBean.java`
- Modify: `ejb/DemoDataBean.java`, `docker-compose.yml`, `src/integrationTest/.../ParkPulseServer.java` (env `PARKPULSE_SIMULATOR=true`)
- Test: `parking/PlateGeneratorTest.java`, `parking/SimulatorPolicyTest.java`, `parking/CarLooksTest.java` (testul de integrare `theSimulatorParksCars` vine în Task 3, odată cu `/api/lot`)

**Interfaces:**
- Consumes: `ParkingSessionBean.enter/exit/findParked`, `ParkingClockBean.now()`, `Spots`, `CarModel`.
- Produces:
  - `final class PlateGenerator { static String next(RandomGenerator random, Set<String> taken); }`. Format: `^[A-Z]{1,2}-\d{2,3}-[A-Z]{3}$`, județe din `SB, B, CJ, BV, AG, TM, IS, MS, HD, VL, AB`; doar `B` primește 3 cifre; literele sunt fără `I`, `O`, `Q`.
  - `final class CarLooks { static final List<String> PALETTE; static CarModel randomModel(RandomGenerator r); static String randomColor(RandomGenerator r, CarModel model); }`. Paleta are 10 culori `#rrggbb`; `TAXI` e mereu `#f5c518`.
  - `final class SimulatorPolicy`:
    - `enum Action { ENTER, EXIT, NOTHING }`
    - `static Action decide(int occupied, int capacity, RandomGenerator r)`
    - `static Optional<String> chooseSpot(Set<String> freeSpots, boolean permit, RandomGenerator r)`
    - `static boolean drawPermit(RandomGenerator r)` (10%)
    - `static Duration nextDelay(RandomGenerator r)` (3–8 s reale)
    - `static String pickLeaving(List<ParkedCarDto> parked, Instant now, RandomGenerator r)` (întoarce locul)
  - `SimulatorBean.tick()`: metoda `@Schedule(hour="*", minute="*", second="*", persistent=false)`, cu `@TransactionAttribute(NOT_SUPPORTED)`, ca fiecare `enter`/`exit` să se comită singur înainte de publicarea evenimentului.
  - Publicarea în fluxul live se adaugă în Task 3, prin injectarea `LiveHub`; aici `tick()` doar loghează fiecare acțiune la nivel FINE.

- [ ] **Step 1: Scrie testele care pică**

```java
// SimulatorPolicyTest (RandomGenerator cu seed fix: new SplittableRandom(7))
@Test void anEmptyLotAlwaysLetsACarIn() { assertEquals(ENTER, SimulatorPolicy.decide(0, 24, rnd)); }
@Test void aFullLotAlwaysLetsACarOut() { assertEquals(EXIT, SimulatorPolicy.decide(24, 24, rnd)); }
@Test void theLotDriftsTowardHalfToSeventyPercent() {
    // 1000 de decizii la 6/24 → majoritatea ENTER; la 20/24 → majoritatea EXIT
    assertTrue(count(ENTER, 6) > 700); assertTrue(count(EXIT, 20) > 700);
}
@Test void aCarWithoutPermitNeverGetsAnAccessibleSpot() {
    assertEquals(Optional.empty(), SimulatorPolicy.chooseSpot(Set.of("A01", "A02"), false, rnd));
    assertEquals(Optional.of("B07"), SimulatorPolicy.chooseSpot(Set.of("A01", "B07"), false, rnd));
}
@Test void aCarWithPermitPrefersAnAccessibleSpot() {
    assertEquals(Optional.of("A02"), SimulatorPolicy.chooseSpot(Set.of("A02", "B07"), true, rnd));
}
@Test void delaysAreThreeToEightSeconds() { for (int i = 0; i < 500; i++) { long ms = SimulatorPolicy.nextDelay(rnd).toMillis(); assertTrue(ms >= 3000 && ms <= 8000); } }

// PlateGeneratorTest
@Test void platesLookRomanianAndAreNotTaken() {
    Set<String> taken = new HashSet<>();
    for (int i = 0; i < 300; i++) { String p = PlateGenerator.next(rnd, taken); assertTrue(p.matches("[A-Z]{1,2}-\\d{2,3}-[A-HJ-NPR-Z]{3}")); assertTrue(taken.add(p)); }
}

// CarLooksTest
@Test void taxisAreYellowAndOtherColorsComeFromThePalette() {
    assertEquals("#f5c518", CarLooks.randomColor(rnd, CarModel.TAXI));
    assertTrue(CarLooks.PALETTE.contains(CarLooks.randomColor(rnd, CarModel.SEDAN)));
}
```

- [ ] **Step 2:** Rulează `./gradlew test`. Expected: FAIL.
- [ ] **Step 3:** Implementează clasele pure.
  - `decide`: sub 50% → ENTER cu p=0.8; peste 70% → EXIT cu p=0.8; altfel 50/50; 0 → ENTER; plin → EXIT.
  - `pickLeaving`: alege la întâmplare dintre mașinile care au stat ≥ 20 de minute de parcare; dacă nu e niciuna, alege cea mai veche.
- [ ] **Step 4:** Rulează `./gradlew test`. Expected: PASS.
- [ ] **Step 5:** Implementează `SimulatorBean` (`@Singleton @Startup @DependsOn("ParkingClockBean")`).
  - Pornește doar dacă `PARKPULSE_SIMULATOR` e `"true"`.
  - `tick()` iese imediat până la momentul următorului eveniment, salvat în memorie pe timp real.
  - La ENTER: alege un loc cu `chooseSpot`; dacă nu e niciunul, face EXIT.
  - Orice excepție e prinsă și logată, ca timer-ul să nu se oprească.
- [ ] **Step 6:** Modifică `DemoDataBean`, cu `@DependsOn("ParkingClockBean")`. Dacă datele demo sunt activate și nu există nicio sesiune, generează:
  - **30 de zile de istoric închis** înainte de `clock.now()`: 90–140 de bilete pe zi, cu ore de intrare între 06:00 și 23:00 și șederi de 5–240 de minute. Folosește `PlateGenerator`, `CarLooks` și `Tariff`. Totul într-o singură tranzacție, printr-o metodă nouă `ParkingSessionBean.importHistory(List<ParkingSession>)`.
  - **12 sesiuni deschise**, intrate în ultimele 3 ore de parcare, pe locuri valide (permisele respectă `chooseSpot`).
- [ ] **Step 7:** În `docker-compose.yml`, la `app`, adaugă `PARKPULSE_SIMULATOR: "true"` și `PARKPULSE_CLOCK_SPEED: "60"`. În `ParkPulseServer`, adaugă `.withEnv("PARKPULSE_SIMULATOR", "true")`.
- [ ] **Step 8: Verificare manuală** (testul automat vine în Task 3, odată cu `/api/lot`). Pornește cu `docker compose up --build` și te uiți în tabel:

```bash
docker compose exec db psql -U parkpulse -c "select count(*) filter (where exited_at is null), count(*) from parking_session;"
```

Expected: cam 12 sesiuni deschise și peste 2500 în total. Rulat din nou după 30 de secunde, numerele se schimbă.
- [ ] **Step 9:** Rulează `./gradlew test integrationTest`. Expected: PASS, testele vechi neatinse.
- [ ] **Step 10:** Iosua verifică, apoi commit `"Simulate cars entering and leaving, with 30 days of demo history"`.

---

### Task 3: Fluxul live (SSE), API-ul public al parcării și pagina live

**Files:**
- Create: `live/LiveHub.java`, `rest/LotResource.java`, `rest/LiveEventsResource.java`, `common/LotSnapshotDto.java`, `styles/app.css`, `scripts/tariff.js`, `scripts/lot-geometry.js`, `scripts/pixel-art.js`, `scripts/lot-renderer.js`, `scripts/live.js`
- Modify:
  - `rest/ParkingLotApplication.java` (`/api`)
  - `WEB-INF/web.xml` (constrângerea `/api/cars/*` → `READ_CARS`, iar `/resources/*` dispare)
  - `security/CsrfFilter.java` (`/api/*` nu creează sesiune și nu e verificat)
  - `ejb/SimulatorBean.java` (publică în `LiveHub`)
  - `index.jsp` (pagina live), `WEB-INF/pages/menu.jsp`, `WEB-INF/tags/pageTemplate.tag`
  - `src/integrationTest/.../ParkPulseIT.java` (`/resources/cars` → `/api/cars`)
  - `Browser.java` (`readFirstSseEvent`)
- Test: IT `theLotIsPublicAndHasNoSessionCookie`, `theLiveFeedStartsWithASnapshot`, `theSimulatorParksCars`, `theCarsApiStillNeedsALogin`

**Interfaces:**
- Consumes: `ParkingSessionBean.findParked/stats/recentActivity`, `ParkingClockBean.now/speed`, `Spots`.
- Produces:
  - `record LotSnapshotDto(Instant clockNow, int clockSpeed, List<String> spots, Set<String> accessibleSpots, List<ParkedCarDto> cars, LotStatsDto stats, List<ActivityDto> activity)`
  - `@ApplicationScoped class LiveHub`:
    - `boolean subscribe(SseEventSink sink, Sse sse, LotSnapshotDto snapshot)`: întoarce `false` peste 200 de abonați. Trimite `snapshot` doar acestui abonat, apoi îl înregistrează în broadcaster-ul creat leneș.
    - `void publish(String name, Object payload)`: JSON prin JSON-B; nu face nimic dacă nu există abonați.
    - Evenimente: `snapshot` (`LotSnapshotDto`), `entered` (`ParkedCarDto`), `exited` (`ActivityDto`), `stats` (`LotStatsDto`).
  - `GET /api/lot` → `LotSnapshotDto` (JSON)
  - `GET /api/events` → `text/event-stream`; 503 când `subscribe` întoarce `false`.
  - JS (ES modules):
    - `tariff.js`: `export function amountBani(seconds)` și `export function formatLei(bani)`, cu aceeași formulă ca `Tariff`.
    - `lot-geometry.js`: `export const W = 320, H = 128`; `export function spotRect(spot) → {x,y,w,h}`; `export function spotAt(x, y) → spot|null`; `export function entryPath(spot)` și `export function exitPath(spot)`, care întorc `[{x,y,reverse}]`; `export const GATES = {entry:{x,y}, exit:{x,y}}`.
    - `pixel-art.js`: `export function carSprite(model, color) → HTMLCanvasElement` (12×20 px, orientată spre nord); `export function drawWheelchair(ctx, x, y)`; `export function drawPixelText(ctx, text, x, y, color)` (font 3×5 pentru `A`, `B`, `0`–`9`).
    - `lot-renderer.js`: `export function createLot(canvas, { reducedMotion }) → { setSnapshot(snapshot), carEntered(car), carExited(activity), select(spot|null) }`.
    - `live.js`: pornește pagina. Deschide `EventSource('/api/events')`, actualizează cardurile și lista de activitate și leagă click-urile (popup-ul de bilet se adaugă în Task 4).

**Geometria hărții** (decizii fixe, în `lot-geometry.js`; pixeli logici, canvas 320×128 scalat cu `image-rendering: pixelated`):
- Zona porților: `x` între 0 și 40, iarbă sus și jos, cabină la (28, 60).
- Locurile au 22 px lățime și 34 px adâncime, începând de la `x=44`:
  - rândul A: `y` de la 10 la 44, mașina intră spre nord, botul în sus;
  - rândul B: `y` de la 84 la 118, mașina intră spre sud, botul în jos.
- Culoarul are `y` între 44 și 84. Banda spre est (intrare) e la `y=74`, banda spre vest (ieșire) la `y=54`.
- Bariera de intrare e pe banda de la `y=74`, la `x=34`. Bariera de ieșire e pe banda de la `y=54`, la `x=34`.
- **Traseu de intrare:** din (−24, 74) spre est până la linia de oprire (`x=22`), apoi așteaptă bariera. Continuă până la (cx−12, 74), virează (Bézier pătratic, cu punctul de control în colț) până la (cx, 62) pentru rândul A sau (cx, 86) pentru rândul B, apoi merge drept până în centrul locului.
- **Traseu de ieșire:** marșarier din centrul locului până la ieșirea din loc, apoi marșarier în viraj până la (cx+12, 54), cu botul spre vest. De acolo merge înainte spre vest până la linia de oprire (`x=46`), așteaptă bariera și iese la (−24, 54).
- **Viteză:** 60 px/s, cu încetinire pe ultimii 10 px. Unghiul mașinii urmează tangenta, cu +180° în marșarier, rotunjit la 22,5°. `imageSmoothingEnabled = false`.
- **Bariere:** se ridică și coboară în 400 ms. Rămân ridicate cât e cineva la coadă.
- **Coada la poartă:** mașinile așteaptă una după alta la linia de oprire. Pe culoar nu facem detecție de coliziuni.
- **Ordinea evenimentelor:** un `exited` care vine în timp ce mașina încă intră termină intrarea pe loc (o pune direct pe loc) și apoi pornește ieșirea.

- [ ] **Step 1: Scrie testele de integrare care pică** (în `ParkPulseIT`):

```java
@Test void theLotIsPublicAndHasNoSessionCookie() {
    HttpResponse<String> r = new Browser(ParkPulseServer.baseUrl()).get("/api/lot");
    assertEquals(200, r.statusCode());
    assertTrue(r.body().contains("\"clockSpeed\":60"));
    assertTrue(r.body().contains("\"A01\""));
    assertTrue(r.headers().allValues("Set-Cookie").stream().noneMatch(c -> c.startsWith("JSESSIONID")));
}
@Test void theLiveFeedStartsWithASnapshot() {
    String first = new Browser(ParkPulseServer.baseUrl()).readFirstSseEvent("/api/events", Duration.ofSeconds(10));
    assertTrue(first.contains("event: snapshot")); assertTrue(first.contains("\"cars\""));
}
@Test void theSimulatorParksCars() {  // așteaptă cel mult 30 s o activitate nouă față de primul snapshot
    ...
}
@Test void theCarsApiStillNeedsALogin() {
    assertFalse(new Browser(ParkPulseServer.baseUrl()).get("/api/cars").body().contains("licensePlate"));
}
@Test void theHomePageIsTheLiveLotForEveryone() {
    String body = new Browser(ParkPulseServer.baseUrl()).get("/").body();
    assertTrue(body.contains("id=\"lot\"")); assertTrue(body.contains("Autentificare"));
}
```

Pentru `theSimulatorParksCars`: citește `/api/lot` și reține `activity.get(0).at`. Apoi interoghează din nou, din 2 în 2 secunde, până la 30 de secunde, și verifică că apare un `at` mai nou.

`Browser.readFirstSseEvent(path, timeout)`: `HttpClient` cu `BodyHandlers.ofLines()`. Citește până la prima linie goală și întoarce textul evenimentului.

Actualizează și testele existente: `/resources/cars` devine `/api/cars` (`theApiNeedsALogin`, `badIdsGive…`, `theApiReturnsCarsAsJson`).
- [ ] **Step 2:** Rulează `./gradlew integrationTest`. Expected: FAIL, pe rutele noi.
- [ ] **Step 3:** Implementează partea de server.
  - `LiveHub`, `LotResource`, `LiveEventsResource` și `ParkingLotApplication` cu `/api`.
  - `web.xml`, unde `/api/cars/*` cere `READ_CARS`.
  - `CsrfFilter`: dacă `getServletPath()` începe cu `/api/`, apelează direct `chain.doFilter`.
  - `SimulatorBean` publică `entered`/`exited` și apoi `stats` după fiecare acțiune.
- [ ] **Step 4:** Implementează pagina.
  - **`pageTemplate.tag`:** adaugă `<link href="${ctx}/styles/app.css">` după Bootstrap și atributul opțional `bodyClass`.
  - **`menu.jsp`, bara nouă:**
    - în stânga: logo-ul pixel (SVG 8×8 din schiță) și „ParkPulse”;
    - în dreapta: „Live”, „API” (`/api-docs`) și linkurile după rol: Mașini (`READ_CARS`), Useri (`READ_USERS`), Încasări (`INVOICING`);
    - fără login: butonul „Autentificare” cu iconiță de user, care duce la `/Login`;
    - cu login: un meniu cu numele userului și „Logout”, care rămâne formularul POST cu `<t:csrf/>`.
  - **`index.jsp`:** layout-ul din `live-page-v4.html`:
    - carduri în stânga: locuri libere / 24 cu bară, încasat azi cu ▲/▼ % față de ieri, mașini azi, durata medie;
    - `<canvas id="lot">` în mijloc, cu indicatorul LIVE;
    - în dreapta, `<ol id="activity">` cu cel mult 20 de rânduri (din `snapshot` vin `recentActivity(20)`, iar rândurile noi se adaugă sus și cele în plus se scot de jos);
    - deasupra hărții, „Ora parcării HH:mm · ×60”, care avansează local între evenimente.
  - **`app.css`:** fundalul din spec (`#eef2f9`, cu pete indigo/turcoaz și puncte fine), cardurile, butoanele „pastilă” și animațiile `rise`, `pulse` și `slide-in`. Pe ecrane sub 900 px, coloanele se pun una sub alta. Respectă `prefers-reduced-motion`.
- [ ] **Step 5:** Rulează `./gradlew integrationTest`. Expected: PASS, toate testele.
- [ ] **Step 6:** Pornește aplicația cu `PARKPULSE_PORT=8081 docker compose up --build` și deschide `http://localhost:8081/`. Verifică vizual:
  - mașini de 5 modele și culori diferite, cu numerele locurilor pe asfalt;
  - 3 locuri albastre cu scaunul cu rotile;
  - o intrare completă: barieră, culoar, viraj, parcare;
  - o ieșire completă: marșarier, culoar, barieră;
  - cardurile și activitatea se actualizează.
- [ ] **Step 7:** Verificări manuale pentru punctul 5 din Review Focus:
  - lasă tab-ul în fundal 2 minute și revino: harta e corectă imediat, fără un șir de animații;
  - activează în Windows „Show animations” = Off: mașinile apar și dispar fără traseu.
  - Implementare: pe `visibilitychange → visible`, `live.js` cere `/api/lot` și apelează `setSnapshot`; evenimentele primite cât tab-ul e ascuns nu pornesc animații.
- [ ] **Step 8:** Iosua verifică în browser și în IntelliJ, apoi commit `"Show the parking lot live in pixel art over server-sent events"`.

---

### Task 4: Biletul: QR, popup animat și pagina `/bilet/{cod}`

**Files:**
- Create: `documents/QrSvg.java`, `rest/TicketsResource.java`, `servlets/TicketPage.java`, `WEB-INF/pages/ticket.jsp`, `scripts/ticket-card.js`, `scripts/ticket-popup.js`, `scripts/ticket-page.js`
- Modify: `build.gradle` (ZXing), `scripts/live.js` (click pe hartă sau pe activitate deschide popup-ul), `app.css`, `docker-compose.yml` (`PARKPULSE_PUBLIC_URL: ${PARKPULSE_PUBLIC_URL:-}`)
- Test: `src/test/.../documents/QrSvgTest.java`; IT `aTicketIsPublicByItsCode`, `aBadTicketCodeIsNotFound`, `theTicketPageOpensForEveryone`, `theQrCodeIsAnSvg`

**Interfaces:**
- Consumes: `ParkingSessionBean.findTicket`, `TicketCodes.looksValid`, `tariff.js`.
- Produces:
  - `final class QrSvg { static String svg(String text); }`: ZXing `QRCodeWriter`, corecție de erori M, zonă liniștită de 2 module.
    - Ieșirea e `<svg xmlns=… viewBox="0 0 N N" shape-rendering="crispEdges">`, cu un `<rect>` alb pe tot fundalul.
    - Apoi, pentru fiecare rând de matrice, un `<g class="qr-row" style="--row:R">` cu câte un `<rect width="1" height="1">` pe fiecare modul închis la culoare.
  - `GET /api/tickets/{code}` → `TicketDto`; 404 dacă `!looksValid(code)` sau biletul nu există.
  - `GET /api/tickets/{code}/qr.svg` → `image/svg+xml`, cu textul `publicUrl + "/bilet/" + code`.
    - `publicUrl` = `PARKPULSE_PUBLIC_URL` fără `/` la final; dacă lipsește, `scheme://host:port` din cerere.
  - `/bilet/{code}` (`TicketPage`): 404 cu pagina de eroare pentru cod invalid sau inexistent; altfel `ticket.jsp`.
  - `ticket-card.js`: `export function renderTicket(container, ticket, qrSvgText) → { update(ticket), destroy() }`. Desenează biletul din schiță:
    - antet indigo „BILET DE PARCARE · PP-xxxxx”;
    - plăcuța cu banda albastră „RO”;
    - chip-ul „● În parcare” sau „Achitat · a ieșit”, plus simbolul de permis, dacă are;
    - Loc, Intrare, Durată, „De plată până acum” / „Plătit”, nota cu tariful;
    - perforația, QR-ul, linkul „Deschide în pagină nouă” și butonul de chitanță.
    - Durata și suma se calculează local: `secundeParcare = (clockNow − enteredAt) + (acumReal − momentulFetch) × clockSpeed`, actualizate la 250 ms. Toate orele se afișează în `Europe/Bucharest` (`Intl.DateTimeFormat('ro-RO', {timeZone:'Europe/Bucharest'})`).
  - `ticket-popup.js`: `export function openTicket(code, fromRect)`, `export function closeTicket()`, `export function ticketExited(activity)`.
    - **FLIP:** animație de 520 ms, `cubic-bezier(.2,.9,.25,1)`, din `fromRect` spre centru. Închiderea durează 340 ms, înapoi spre `fromRect`.
    - **Fundal:** estompat, cu `blur(3px)`.
    - **QR:** rândurile apar cu `animation-delay: calc(380ms + var(--row) * 22ms)`, iar o linie de scanare indigo coboară peste ele.
    - **Închidere:** cu X, Esc sau click pe fundal. Focusul merge pe X la deschidere și revine pe elementul de pornire la închidere.
    - **Loc liber:** un toast cu „Locul A03 e liber”.

- [ ] **Step 1: Scrie testele care pică**

```java
// QrSvgTest
@Test void everyMatrixRowIsAGroupInsideAQuietZone() {
    String svg = QrSvg.svg("http://localhost:8081/bilet/AAAAAAAAAAAAAAAAAAAAAA");
    Matcher m = Pattern.compile("viewBox=\"0 0 (\\d+) \\1\"").matcher(svg); assertTrue(m.find());
    int size = Integer.parseInt(m.group(1));
    assertEquals(size - 4, svg.split("class=\"qr-row\"", -1).length - 1);
}
// IT
@Test void aTicketIsPublicByItsCode() { /* ia un cod din /api/lot → GET /api/tickets/{cod} = 200, conține "PP-" și plăcuța */ }
@Test void aBadTicketCodeIsNotFound() {
    Browser b = new Browser(ParkPulseServer.baseUrl());
    assertEquals(404, b.get("/api/tickets/abc").statusCode());
    assertEquals(404, b.get("/api/tickets/" + "x".repeat(500)).statusCode());
    assertEquals(404, b.get("/bilet/AAAAAAAAAAAAAAAAAAAAAA").statusCode());
}
@Test void theQrCodeIsAnSvg() { /* Content-Type începe cu image/svg+xml, body conține "qr-row" */ }
@Test void theTicketPageOpensForEveryone() { /* GET /bilet/{cod din /api/lot} = 200, conține data-ticket-code */ }
```

- [ ] **Step 2:** Rulează `./gradlew test integrationTest`. Expected: FAIL.
- [ ] **Step 3:** Adaugă `implementation 'com.google.zxing:core:3.5.4'` și implementează partea de server.
- [ ] **Step 4:** Implementează JS-ul și CSS-ul biletului.
  - `live.js` leagă click-ul pe hartă (`spotAt`): un loc ocupat deschide `openTicket(car.ticketCode, rectul locului pe ecran)`, iar unul liber afișează toast-ul.
  - Un rând din activitate deschide biletul pornind de la rectul rândului.
  - Un `exited` pentru biletul deschis apelează `ticketExited`.
  - `ticket-page.js` desenează biletul pe toată pagina, gândită pentru telefon, și reîncarcă `/api/tickets/{cod}` la fiecare 5 secunde cât mașina e în parcare.
- [ ] **Step 5:** Rulează `./gradlew test integrationTest`. Expected: PASS.
- [ ] **Step 6:** Verifică manual pe `http://localhost:8081/`:
  - click pe mașină: animația de deschidere, QR-ul care se „printează”, suma care crește cu 10 bani pe secundă după minutul 15 de parcare;
  - Esc închide biletul;
  - „Deschide în pagină nouă” deschide `/bilet/{cod}`;
  - QR-ul scanat cu telefonul arată adresa corectă (pe `localhost` nu se deschide pe telefon, iar asta e normal).
- [ ] **Step 7:** Iosua verifică, apoi commit `"Open an animated ticket with a scannable QR code"`.

---

### Task 5: Chitanța PDF

**Files:**
- Create: `documents/PdfFonts.java`, `documents/ReceiptPdf.java`
- Modify: `build.gradle` (OpenPDF, fonts-extra), `rest/TicketsResource.java`, `scripts/ticket-card.js` (butonul descarcă `receipt.pdf`), `Browser.java` (`getBytes`)
- Test: `documents/ReceiptPdfTest.java`; IT `aReceiptIsAPdfForAFinishedTicket`, `aReceiptForAParkedCarIsAConflict`

**Interfaces:**
- Consumes: `TicketDto`, `Tariff.format`, `AcceleratedClock.ZONE`.
- Produces:
  - `final class PdfFonts { static Font regular(float size); static Font bold(float size); }`: Liberation Sans din `openpdf-fonts-extra`, cu `BaseFont.IDENTITY_H` și încorporat în PDF.
  - `final class ReceiptPdf { static byte[] render(TicketDto ticket); }`:
    - lățime de 80 mm (226,77 pt), înălțimea potrivită conținutului;
    - titlul „ParkPulse · Chitanță”, apoi numărul biletului, plăcuța, locul, intrarea, ieșirea, durata, tariful („primele 15 minute gratuit, apoi 6 lei/oră”), totalul și „Mulțumim!”.
  - `GET /api/tickets/{code}/receipt.pdf` → `application/pdf` cu `Content-Disposition: attachment; filename="chitanta-PP-xxxxx.pdf"`. Dacă mașina e încă în parcare, răspunde 409 cu textul „Chitanța apare după ieșire”.

- [ ] **Step 1:** Găsește calea fontului:

```bash
./gradlew dependencies --configuration runtimeClasspath | grep openpdf
unzip -l ~/.gradle/caches/modules-2/files-2.1/com.github.librepdf/openpdf-fonts-extra/3.0.5/*/openpdf-fonts-extra-3.0.5.jar | grep -i 'liberation.*sans.*\.ttf'
```

Expected: căile pentru `LiberationSans-Regular.ttf` și `LiberationSans-Bold.ttf`, folosite în `PdfFonts`.
- [ ] **Step 2: Scrie testele care pică**

```java
// ReceiptPdfTest – textul se citește înapoi cu PdfReader + PdfTextExtractor din OpenPDF
@Test void theReceiptShowsTheTicketWithRomanianLetters() {
    TicketDto t = new TicketDto("AAAAAAAAAAAAAAAAAAAAAA", "PP-00042", "SB-47-XYZ", "A08", CarModel.SEDAN, "#3498db", false,
        Instant.parse("2026-10-27T10:00:00Z"), Instant.parse("2026-10-27T11:58:00Z"), 1030, Instant.parse("2026-10-27T12:00:00Z"), 60);
    String text = textOf(ReceiptPdf.render(t));
    assertTrue(text.contains("PP-00042")); assertTrue(text.contains("SB-47-XYZ")); assertTrue(text.contains("10,30 lei"));
    assertTrue(text.contains("Chitanță")); assertTrue(text.contains("13:58"));   // ora României (UTC+2)
}
// IT
@Test void aReceiptIsAPdfForAFinishedTicket() { /* cod din activity cu type=exited → 200, Content-Type application/pdf, primii octeți "%PDF" */ }
@Test void aReceiptForAParkedCarIsAConflict() { /* cod din cars → 409 */ }
```

- [ ] **Step 3:** Rulează testele. Expected: FAIL.
- [ ] **Step 4:** Adaugă dependențele și implementează `PdfFonts`, `ReceiptPdf` și ruta.
- [ ] **Step 5:** Rulează `./gradlew test integrationTest`. Expected: PASS.
- [ ] **Step 6:** Descarcă manual o chitanță din popup și deschide-o: diacriticele apar corect, iar textul încape pe 80 mm.
- [ ] **Step 7:** Iosua verifică, apoi commit `"Download a PDF receipt for finished tickets"`.

---

### Task 6: Pagina de încasări, raportul PDF și scoaterea bifelor „Invoice”

**Files:**
- Create: `parking/BillingPeriod.java`, `ejb/BillingBean.java`, `common/PaidTicketDto.java`, `common/DayTotalDto.java`, `common/BillingSummaryDto.java`, `common/PageDto.java`, `rest/BillingResource.java`, `documents/ReportPdf.java`, `servlets/Billing.java`, `WEB-INF/pages/billing.jsp`
- Modify:
  - `web.xml` (`/api/billing/*` → `INVOICING`)
  - `servlets/users/Users.java` (fără `doPost` și `InvoiceBean`)
  - `WEB-INF/pages/users/users.jsp` (fără formularul de factură și secțiunea „Invoices Selection”)
  - `ParkPulseIT` (înlocuiește `billingCanPickUsersForAnInvoice` și aserțiunea `contains("Invoice")` din `adminCanManageCarsAndUsers`)
- Delete: `ejb/InvoiceBean.java`
- Test: `parking/BillingPeriodTest.java`, `documents/ReportPdfTest.java`; IT `billingSeesTheBillingPage`, `viewerCannotSeeBilling`, `badBillingDatesGiveBadRequest`, `theBillingApiListsPaidTickets`

**Interfaces:**
- Consumes: `ParkingSession`, `ParkingClockBean`, `TicketCodes.displayNumber`, `PdfFonts`, `Tariff.format`.
- Produces:
  - `record PaidTicketDto(String ticketCode, String ticketNumber, String plate, String spot, Instant enteredAt, Instant exitedAt, int amountBani)`
  - `record DayTotalDto(LocalDate day, int totalBani, int tickets)`
  - `record BillingSummaryDto(LocalDate from, LocalDate to, int totalBani, int previousPeriodTotalBani, int paidTickets, int averageBani, int freeTickets, List<DayTotalDto> days)`
    - `days` conține fiecare zi din interval, inclusiv zilele cu 0.
  - `record PageDto<T>(List<T> items, int page, int pageSize, long total)`, cu `pageSize` = 20 și `page` pornind de la 1.
  - `BillingBean`:
    - `BillingSummaryDto summary(LocalDate from, LocalDate to)`
    - `PageDto<PaidTicketDto> paidTickets(LocalDate from, LocalDate to, String plateContains, int page)`
    - `List<PaidTicketDto> allPaidTickets(LocalDate from, LocalDate to)`, pentru raport
    - Intervalul e `[from 00:00, to+1 00:00)` în `Europe/Bucharest`. „Plătit” înseamnă `exitedAt != null`. Căutarea nu ține cont de majuscule și ignoră cratimele. Ordinea e `exitedAt` descrescător.
  - `final class BillingPeriod { static BillingPeriod parse(String from, String to, LocalDate today); LocalDate from(); LocalDate to(); }`, în `parking/`, testată unitar. Aruncă `IllegalArgumentException` pentru format greșit, `from > to` sau mai mult de 366 de zile. Implicit: ultimele 7 zile (`today−6 … today`).
  - REST: `GET /api/billing/tickets?from=&to=&plate=&page=` → `PageDto<PaidTicketDto>`, iar `GET /api/billing/report.pdf?from=&to=` → PDF. Pentru `IllegalArgumentException` răspunde 400.
  - `ReportPdf.render(BillingSummaryDto summary, List<PaidTicketDto> tickets) → byte[]`: A4 cu titlul „Raport de încasări” și perioada, totalurile, un tabel pe zile și tabelul biletelor, cu antet repetat pe fiecare pagină.
  - `/billing` (servlet, `@ServletSecurity(rolesAllowed = INVOICING)`) are parametrii `period=today|7d|month|custom`, `from`, `to`, `plate`, `page`. `billing.jsp` e desenat pe server după `billing-page.html`:
    - filtrul de perioadă ca grup de butoane pastilă, plus `<input type="date">` pentru varianta custom;
    - butonul „Raport PDF pe perioadă” (link spre `/api/billing/report.pdf?from&to`);
    - 4 carduri: încasat cu ▲/▼ % față de perioada anterioară, bilete plătite, medie pe bilet, gratuite;
    - graficul cu bare CSS (înălțimea = `totalBani / max × 100%`, ziua de azi în turcoaz, animația `grow` cu `prefers-reduced-motion`);
    - tabelul, cu „gratuit” pentru 0 bani, link PDF pe fiecare rând, căutarea (formular GET) și paginarea.

- [ ] **Step 1: Scrie testele care pică**

```java
// BillingPeriodTest (în src/test/.../parking)
@Test void theDefaultIsTheLastSevenDays() { var p = BillingPeriod.parse(null, null, LocalDate.of(2026,10,27)); assertEquals(LocalDate.of(2026,10,21), p.from()); assertEquals(LocalDate.of(2026,10,27), p.to()); }
@Test void badDatesAreRejected() {
    LocalDate today = LocalDate.of(2026,10,27);
    assertThrows(IllegalArgumentException.class, () -> BillingPeriod.parse("ieri", "2026-10-27", today));
    assertThrows(IllegalArgumentException.class, () -> BillingPeriod.parse("2026-10-27", "2026-10-01", today));
    assertThrows(IllegalArgumentException.class, () -> BillingPeriod.parse("2024-01-01", "2026-10-27", today));
}
// ReportPdfTest
@Test void theReportListsTotalsAndDays() { /* summary cu 2 zile + 3 bilete → textul conține „Raport de încasări”, „Total”, suma formatată și fiecare PP-xxxxx */ }
// IT
@Test void billingSeesTheBillingPage() { assertTrue(Browser.loggedInAs("billing").get("/billing").body().contains("Încasări")); }
@Test void viewerCannotSeeBilling() { Browser v = Browser.loggedInAs("viewer"); assertEquals(403, v.get("/billing").statusCode()); assertEquals(403, v.get("/api/billing/tickets").statusCode()); }
@Test void badBillingDatesGiveBadRequest() { assertEquals(400, Browser.loggedInAs("billing").get("/api/billing/tickets?from=ieri").statusCode()); }
@Test void theBillingApiListsPaidTickets() { /* billing → 200, JSON cu "items" și "total" > 0 (istoricul demo) */ }
@Test void theReportIsAPdf() { /* billing → /api/billing/report.pdf → %PDF */ }
```

- [ ] **Step 2:** Rulează testele. Expected: FAIL.
- [ ] **Step 3:** Implementează `BillingPeriod`, `BillingBean`, `ReportPdf`, ruta REST, servletul și JSP-ul. Șterge partea de factură veche.
- [ ] **Step 4:** Rulează `./gradlew test integrationTest`. Expected: PASS, inclusiv testele vechi modificate.
- [ ] **Step 5:** Verifică manual, logat ca `billing`: perioadele, căutarea după număr, paginarea, raportul PDF și chitanța de pe un rând.
- [ ] **Step 6:** Iosua verifică, apoi commit `"Add a billing page with daily totals and a PDF report"`.

---

### Task 7: OpenAPI și `/api-docs` cu Swagger UI

**Files:**
- Create: `servlets/ApiDocs.java`, `WEB-INF/pages/api-docs.jsp`, `scripts/api-docs.js`
- Modify: `build.gradle`, `rest/ParkingLotApplication.java` (`@OpenAPIDefinition`), toate resursele REST (`@Tag`, `@Operation`, `@APIResponse`, `@Parameter` cu exemple)
- Test: IT `theOpenApiDocumentListsTheRoutes`, `theApiDocsPageLoadsSwaggerUi`

**Interfaces:**
- Produces:
  - `@OpenAPIDefinition(info = @Info(title = "ParkPulse API", version = "1.0", description = "…"))`, cu tag-urile „Parcare”, „Bilete”, „Mașini” și „Încasări”. Descrierea lui `/api/events` spune că e un flux SSE și că „Try it” nu îl poate afișa.
  - `/api-docs` (servlet public) → `api-docs.jsp`. Pagina încarcă `/webjars/swagger-ui/5.33.2/swagger-ui.css` și `swagger-ui-bundle.js`, iar `api-docs.js` apelează `SwaggerUIBundle({ url: '/openapi', dom_id: '#swagger' })`. Pagina folosește bara din `menu.jsp`.

- [ ] **Step 1: Scrie testele care pică**

```java
@Test void theOpenApiDocumentListsTheRoutes() {
    String doc = new Browser(ParkPulseServer.baseUrl()).get("/openapi?format=json").body();
    for (String path : List.of("/api/lot", "/api/events", "/api/tickets/{code}", "/api/tickets/{code}/receipt.pdf", "/api/billing/tickets", "/api/cars"))
        assertTrue(doc.contains("\"" + path + "\""), path);
    assertTrue(doc.contains("ParkPulse API"));
}
@Test void theApiDocsPageLoadsSwaggerUi() {
    Browser b = new Browser(ParkPulseServer.baseUrl());
    assertTrue(b.get("/api-docs").body().contains("swagger-ui-bundle.js"));
    assertEquals(200, b.get("/webjars/swagger-ui/5.33.2/swagger-ui-bundle.js").statusCode());
}
```

- [ ] **Step 2:** Rulează `./gradlew integrationTest`. Expected: FAIL. Dacă `/openapi` întoarce 404 sau 403, serviciul e oprit în imagine. Atunci se pornește din `Dockerfile`, cu o comandă `asadmin set-openapi-configuration --enabled=true` într-un script `post-boot` (`${POSTBOOT_COMMANDS}`). Verifică dacă e cazul înainte să schimbi ceva.
- [ ] **Step 3:** Adaugă dependențele și adnotările, apoi pagina.
- [ ] **Step 4:** Rulează `./gradlew integrationTest`. Expected: PASS.
- [ ] **Step 5:** Deschide manual `http://localhost:8081/api-docs` și încearcă „Try it” pe `/api/lot`.
- [ ] **Step 6:** Iosua verifică, apoi commit `"Document the API with OpenAPI and Swagger UI"`.

---

### Task 8: Stilul nou pentru paginile vechi și README

**Files:**
- Modify: `app.css`, `login.jsp`, `about.jsp`, `footer.jsp`, `cars/*.jsp`, `users/*.jsp`, `README.md`
- Test: suita existentă de integrare (textele pe care se bazează testele rămân sau se actualizează în același commit)

- [ ] **Step 1:** Rescrie paginile vechi cu clasele din `app.css` (`.pp-card`, `.pp-btn`, `.pp-btn-primary`, `.pp-table`, `.pp-form`), pe același fundal și cu același antet.
  - Formularele își păstrează câmpurile, `<t:csrf/>` și validarea din `form_validation.js`.
  - Login-ul e un card centrat, cu logo.
  - Textele rămân în engleză acolo unde testele le caută (`"Sign in"`). Dacă le traduci, actualizează testele în același commit.
- [ ] **Step 2:** Actualizează README.
  - Secțiunea „Ce vezi”: pagina live, biletul, încasările și `/api-docs`.
  - „Conturi demo”: `admin`, `viewer`, `billing` / `parkpulse-demo` și ce poate face fiecare.
  - Notă: codurile biletelor apar pe hartă pentru că mașinile sunt simulate.
  - Variabilele noi: `PARKPULSE_SIMULATOR`, `PARKPULSE_CLOCK_SPEED`, `PARKPULSE_PUBLIC_URL`.
- [ ] **Step 3:** Rulează `./gradlew test integrationTest`. Expected: PASS.
- [ ] **Step 4:** Trecere manuală prin toate paginile (logat ca `admin`, `viewer`, `billing` și nelogat), pe desktop și pe lățime de telefon (DevTools, 390 px).
- [ ] **Step 5:** Iosua verifică, apoi commit `"Give the older pages the new look and update the README"`.

---

### La final

- [ ] Review pe tot branch-ul (agentul `reviewer` sau `/code-review`), apoi corecturi.
- [ ] Push pe `faza-3`. PR-ul îl deschide Iosua din link (tokenul MCP nu poate crea PR-uri).
- [ ] CI verde pe PR.
