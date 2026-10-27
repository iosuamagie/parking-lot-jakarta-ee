# Faza 3 – Parcarea live: design

Data: 2026-10-27 · Branch: `faza-3` · Autor: Iosua (decizii) + Claude (propuneri marcate „decizia lui Claude”)

## 1. Ce vrem și de ce

ParkPulse (fostul laborator ULBS „parking-lot-jakarta-ee”) devine proiectul de portofoliu de pe profilul GitHub al lui Iosua. Fazele 0–2 au făcut aplicația sigură, au pus-o în Docker și au acoperit-o cu teste și CI. Faza 3 trebuie să facă aplicația **vie și impresionantă pentru un recrutor care deschide link-ul**: o parcare publică în care mașinile intră și ies singure, vizibilă live, în pixel art, fără cont, cu bilet QR, tarif, chitanță PDF, o pagină de încasări pentru personal și documentație de API.

Proiectul trebuie să rămână **un proiect Jakarta EE „de facultate”**: Java, Jakarta EE, JSP, fără framework-uri de frontend.

## 2. Obiective

1. **Parcare publică simulată:** la intervale aleatorii (3–8 s) intră o mașină cu număr inventat sau iese una deja parcată. Parcarea are **24 de locuri** (A01–A12, B01–B12) și stă de obicei între 50% și 70% plină.
2. **Pagina live** (`/`, fără cont), cu layout-ul aprobat în schița `live-page-v4`:
   - sus: „ParkPulse” cu logo pixel în stânga; în dreapta butonul „API” și butonul „Autentificare” cu iconiță de user;
   - stânga: carduri unul sub altul (locuri libere / 24 cu bară, încasat azi cu comparație față de ieri, mașini azi, durată medie);
   - mijloc: parcarea mare, desenată în pixel art pe Canvas 2D, cu indicator „LIVE” care pulsează;
   - dreapta: lista „Activitate” cu ultimele 20 de evenimente; cele noi alunecă înăuntru și se luminează;
   - fundal: alb-albăstrui (`#eef2f9`) cu pete de culoare indigo/turcoaz și puncte fine (varianta „puncte fine” din schița v3);
   - animații: cardurile urcă la încărcare, mașinile intră și ies pe drum, iar o mașină care parchează „apare” pe loc. Totul pornește de la date live, nu în buclă.
3. **Bilet:** click pe o mașină de pe hartă sau pe un rând din activitate deschide biletul cu animație FLIP: biletul „crește” din elementul pe care ai dat click, fundalul se estompează, iar QR-ul se „printează” rând cu rând, cu o linie de scanare. Biletul are: număr afișat (`PP-04821`), numărul mașinii în stil plăcuță RO, locul, ora intrării, durata și suma, care cresc live, QR-ul, un link „Deschide în pagină nouă” și butonul de chitanță (gri cât mașina e în parcare, activ după ieșire, când biletul arată „Achitat”). Se închide cu X, cu Esc sau cu click pe fundal; la închidere se strânge înapoi. Pe un loc liber apare doar mesajul „Locul A03 e liber”.
4. **Pagina biletului** `/bilet/{cod}`: publică, gândită pentru telefon, cu aceleași date live; spre ea duce QR-ul.
5. **Tarif:** primele 15 minute gratuit, apoi **6 lei/oră, socotit pe fiecare minut început** (0,10 lei/minut). Sumele se țin în **bani** (întregi).
6. **Chitanță PDF** pentru un bilet încheiat: `/api/tickets/{cod}/receipt.pdf`, publică pentru cine are codul.
7. **Pagina de încasări** `/billing` (doar rolul `INVOICING`, adică `billing` și `admin`), după schița `billing-page`: filtru de perioadă (azi / ultimele 7 zile / luna asta / interval ales), carduri (încasat în perioadă cu comparație, bilete plătite, medie pe bilet, gratuite sub 15 min), grafic cu bare pe zile (CSS, fără bibliotecă), tabel de bilete plătite cu căutare după număr, paginare și link PDF pe fiecare rând, plus butonul „Raport PDF pe perioadă”.
8. **API REST** (JSON) documentat prin MicroProfile OpenAPI (inclus în Payara, la `/openapi`), cu pagina **`/api-docs`** făcută cu **Swagger UI** (fișierele puse în WAR, versiune fixată) și butonul „Try it”:
   - public: `GET /api/lot`, `GET /api/events` (SSE), `GET /api/tickets/{cod}`, `GET /api/tickets/{cod}/qr.svg`, `GET /api/tickets/{cod}/receipt.pdf`;
   - cu cont: `GET /api/cars`, `GET /api/cars/{id}` (există deja, rol `READ_CARS`); `GET /api/billing/tickets?from=&to=&plate=&page=` și `GET /api/billing/report.pdf?from=&to=` (rol `INVOICING`);
   - fiecare rută are descriere, exemple și coduri de răspuns.
9. **Stil comun:** paginile vechi (login, mașini, useri, adăugare/editare) primesc același stil ca pagina live: carduri albe rotunjite, butoane „pastilă”, același font și fundal. Bootstrap 5.3.8 (CDN cu SRI, cum e acum) rămâne dedesubt, iar peste el vine un singur `app.css` al nostru, cu variabile de culoare.
10. **Bifele vechi de „Invoice”** de pe pagina de useri (`InvoiceBean`, POST pe `/Users`, partea din `users.jsp`) dispar, fiindcă le înlocuiește `/billing`.

## 3. Ce NU facem (non-goals)

- Plăți reale, conturi pentru șoferi, rezervări, abonamente.
- Schimbări de model pentru tabelul vechi `Car` (proprietar + loc fix): rămâne cum e, cu paginile lui; simulatorul nu îl atinge.
- Temă pixel pe toată aplicația: doar harta și logo-ul sunt pixel art.
- Demo-ul public pe internet (rămâne opțional, în faza 4), README cu GIF, licență MIT (tot faza 4).

## 4. Anti-obiective (ce ar face faza un eșec chiar dacă merge)

- Apar Node.js, npm, React sau orice framework de frontend. Doar JSP, JavaScript simplu și CSS.
- Pagina live sau biletul cer login.
- Codul unui bilet se poate ghici (număr la rând în URL).
- Se strică testele sau paginile existente, ori `docker compose up` nu mai pornește totul dintr-o comandă.
- Biblioteci sau imagini adăugate fără acordul lui Iosua, sau versiuni nefixate (`latest`, versiuni mai noi de 2 săptămâni).
- Animații care deranjează: maximum 1–2 lucruri animate în același timp, iar `prefers-reduced-motion` e respectat peste tot.
- Commituri cu atribuire Claude (commiturile sunt doar pe numele lui Iosua).

## 5. Constrângeri

- Java 21, Jakarta EE 10 API, Payara 7.2026.9, PostgreSQL 17, Gradle, Docker Compose (faza 1), CI pe GitHub Actions (faza 2).
- Fără imagini descărcate (toate desenele sunt în cod, vezi §10 Q2). Dependențe noi, cu „da” explicit de la Iosua înainte de adăugare și cu versiuni verificate la momentul planului (nu din memorie): **ZXing core** (QR, Apache 2.0), **OpenPDF** (PDF, LGPL/MPL), **Swagger UI** (fișiere statice, Apache 2.0).
- Pe PC-ul lui Iosua portul 8080 e ocupat, așa că rulează cu `PARKPULSE_PORT=8081`.
- Testele de integrare (Testcontainers) folosesc HTTP/1.1 și urmează redirect-ul după `j_security_check`.
- Iosua vede modificările în IntelliJ înainte de fiecare commit.

## 6. Variante luate în calcul

| Variantă | Pe scurt | Ce câștigi / ce pierzi |
|---|---|---|
| **A. Totul în aplicația de acum** ✅ aleasă | JSP + JavaScript simplu + Canvas 2D + SSE din JAX-RS | Un proiect, o comandă, CI neschimbat, stil comun cu paginile vechi; JavaScript scris de mână |
| B. Frontend React separat | Vite + React; Java doar API | Arată bine în CV, dar aduce Node/npm, un build în plus și două lumi diferite (JSP + React); dublează munca |
| C. JSP + htmx/Alpine | O bibliotecă mică peste JSP | Ajută puțin la o pagină de Canvas + SSE; o dependență în plus fără câștig real |

Iosua a ales **A** și **Swagger UI** (nu Scalar), pentru că vrea ca proiectul să rămână doar Java/Jakarta EE.

## 7. Ce s-a decis despre „cum”

### 7.1 Date
- Entitate nouă **`ParkingSession`** (tabel `parking_session`): `id` (secvență), `ticketCode` (unic, aleatoriu, cel puțin 128 de biți din `SecureRandom`, base64url), `licensePlate`, `spot` (`A01`…`B12`), `enteredAt`, `exitedAt` (null cât e în parcare), `amountBani` (calculat la ieșire). Numărul afișat `PP-xxxxx` vine din `id`.
- Un loc are cel mult o sesiune deschisă; o plăcuță are cel mult o sesiune deschisă. Regula e păzită în aplicație: toate scrierile trec prin bean-ul singleton al simulatorului, deci sunt una după alta. Nu folosim index parțial, pentru că `create-or-extend-tables` nu îl poate crea. *Decizia lui Claude.*
- **O singură instanță:** aplicația rulează într-un singur container Payara (compose). Simulatorul `@Singleton`, `SseBroadcaster`-ul din memorie și regula de mai sus se bazează pe asta, deci nu facem blocări la nivel de bază de date. *Decizia lui Claude.*
- **La repornire:** sesiunile deschise rămân deschise, iar simulatorul le continuă (ceasul parcării continuă de la ultimul moment salvat, vezi §10 Q1, deci nu apar durate absurde). *Decizia lui Claude.*
- Tariful e o clasă Java pură (fără EJB), testată unitar, folosită și de server, și (prin aceeași formulă) de JavaScript-ul care afișează suma live.

### 7.2 Simulator
- Bean `@Singleton @Startup` cu `@Schedule(..., persistent = false)` care rulează des (de exemplu la 1 s) și decide la intervale aleatorii de 3–8 s: intrare sau ieșire, în funcție de cât de plină e parcarea (ținta 50–70%).
- Numere inventate în format românesc (`SB-47-XYZ`), fără dubluri cu mașinile din parcare.
- Pornit/oprit cu variabila `PARKPULSE_SIMULATOR` (implicit `true` în `docker-compose.yml`, `false` în testele de integrare care nu au nevoie de el).
- **Timp:** ceasul parcării accelerat ×60, vezi §10 Q1.

### 7.3 Live (SSE)
- `GET /api/events`: `Sse` + `SseBroadcaster` din JAX-RS, într-un bean singleton. La conectare se trimite un eveniment `snapshot` (toate locurile + cifrele), apoi `entered`, `exited` și `stats`.
- Browserul folosește `EventSource`, care se reconectează singur; la reconectare primește din nou `snapshot`.
- Limită de conexiuni SSE simultane (de exemplu 200), ca o pagină publică să nu poată epuiza serverul. *Decizia lui Claude.*

### 7.4 REST și securitate
- `@ApplicationPath` se mută din `/resources` în **`/api`**; constrângerea din `web.xml` și testele existente (`/resources/cars`) se actualizează, iar `/resources/*` nu rămâne ca alias. *Decizia lui Claude: o singură rădăcină de API, mai curată în documentație.*
- **Pagina principală:** `/` devine pagina live (înlocuiește `index.jsp`). Meniul de sus e același pe toate paginile: logo în stânga; în dreapta „Live”, „API” și, după login, linkurile la care ai acces după rol (Mașini, Useri, Încasări), plus un meniu cu numele userului și „Logout”. `about.jsp` rămâne, cu stilul nou. *Decizia lui Claude.*
- **Codurile biletelor în fluxul public:** `snapshot`/`entered` din `/api/events` și `/api/lot` includ `ticketCode` pentru fiecare mașină parcată, ca să meargă click pe hartă → bilet. Deci, în demo, orice bilet e accesibil de pe hartă, ceea ce e în regulă, pentru că toate mașinile sunt simulate. Codul rămâne totuși aleatoriu și negăsibil, ca model corect pentru o parcare reală, iar listarea biletelor plătite (`/api/billing/*`) rămâne doar pentru `INVOICING`. Asta se scrie în README. *Decizia lui Claude.*
- Rutele publice (`/`, `/bilet/*`, `/api/lot`, `/api/events`, `/api/tickets/*`, `/api-docs`, `/openapi`, fișierele statice) sunt explicit fără constrângere; `/api/billing/*` și `/billing` cer `INVOICING`.
- Filtrul CSRF existent rămâne pentru formulare; noile rute publice sunt doar `GET`.

### 7.5 QR
- ZXing generează matricea; serverul o dă ca **SVG cu câte un pătrățel pe modul**, grupat pe rânduri, ca animația de „printare” rând cu rând să meargă din CSS. *Decizia lui Claude.*
- Link-ul din QR e absolut: `PARKPULSE_PUBLIC_URL` + `/bilet/{cod}`. Dacă variabila lipsește, se folosește adresa cererii. Notă: de pe telefon, scanarea merge doar dacă adresa e accesibilă din rețeaua telefonului (pe `localhost` nu).

### 7.6 PDF
- OpenPDF. **Chitanța:** format îngust, ca un bon de parcare (de exemplu 80 mm lățime), cu numărul biletului, mașina, locul, intrarea, ieșirea, durata, tariful și suma. **Raportul pe perioadă:** A4, cu totaluri, bilete pe zile și tabelul biletelor. *Formatul exact e decizia lui Claude.*
- Fontul din PDF trebuie să afișeze diacriticele românești (ă, î, ș, ț); fontul se pune în aplicație dacă cel implicit nu le are.

### 7.7 Frontend
- JSP + un `app.css` (variabile de culoare, carduri, butoane „pastilă”, fundal) + câte un fișier JavaScript pe pagină (`live.js`, `ticket.js`, `billing.js`), fără bibliotecă.
- Harta e desenată pe Canvas 2D, cu `image-rendering: pixelated`. Click-ul pe hartă se transformă în coordonate de loc, ca în schiță.
- **Mașinile, marcajele și animațiile:** vezi §10 Q2 și „Animațiile hărții”.
- Pe telefon, coloanele paginii live se așază una sub alta (carduri, hartă, activitate).
- Font: fontul sistemului (`system-ui`, `Segoe UI`); nu se încarcă fonturi externe. *Decizia lui Claude.*

### 7.8 Date demo
- Când `PARKPULSE_DEMO_DATA=true` și nu există sesiuni, se generează un **istoric de 30 de zile** de bilete plătite, ca pagina `/billing` și cardurile „față de ieri” să aibă date de la prima pornire. *Decizia lui Claude.*

## 8. Ordinea de lucru (câte un commit pe pas)

1. `ParkingSession` + tarif (+ teste unitare)
2. Simulator
3. SSE + pagina live
4. Bilet: QR, pagina `/bilet/{cod}`, animația
5. Chitanța PDF
6. Pagina `/billing` + raport PDF; scoaterea bifelor „Invoice”
7. OpenAPI + `/api-docs` cu Swagger UI
8. Stilul nou pentru paginile vechi

Fiecare pas are teste: unitare pentru logică (tarif, ceas, coduri de bilet, alegerea intrare/ieșire) și de integrare pentru rute (acces public vs. roluri, tipuri de conținut PDF/SVG, primul eveniment SSE, `/openapi` care listează rutele).

## 9. Lăsat la latitudinea constructorului

Numele exacte de clase și pachete, structura JSP-urilor, culorile mașinilor, textele mesajelor, mărimea paginilor din tabele, detaliile de animație în limitele din §4, structura evenimentelor SSE.

## 10. Întrebările deschise, rezolvate de Iosua

### Q1 – Ceasul parcării: accelerat (×60)
- **1 secundă reală = 1 minut în parcare.** Pe pagina live scrie „Ora parcării 14:37 · ×60”. Pe bilet, durata crește cu un minut și suma cu 10 bani în fiecare secundă reală (după cele 15 minute gratuite).
- **Toate orele salvate și afișate sunt pe ceasul parcării:** `enteredAt`/`exitedAt`, biletul, pagina biletului, chitanța și raportul PDF, „azi”/„ieri” de pe pagina live, filtrele din `/billing` și istoricul demo. Nu există amestec cu ora reală.
- Ceasul e o clasă separată (`ParkingClock` sau similar), injectată peste tot unde e nevoie de „acum”, ca testele să-l poată controla. Viteza vine din `PARKPULSE_CLOCK_SPEED` (implicit `60`).
- **Persistență:** la pornire, ceasul continuă de la cel mai recent moment salvat (ultimul `enteredAt`/`exitedAt` din bază), nu sare. Dacă baza e goală, pornește de la ora reală.
- Istoricul demo de 30 de zile (§7.8) se generează pe același ceas, înainte de „acum”-ul ceasului.
- O „zi” în parcare durează 24 de minute reale, deci graficul din `/billing` se umple repede.

### Q2 – Mașinile și harta: desenate de noi, pixel art, fără descărcări
- Nu se folosește niciun pachet Kenney. Toate desenele (mașini, bariere, marcaje, simbolul de persoane cu dizabilități) sunt făcute în cod, pe Canvas 2D, pixel cu pixel. Sunt 100% ale proiectului.
- **Modele de mașini, văzute de sus:** cel puțin 5 (mașină mică, sedan, SUV, taxi, dubiță), fiecare cu faruri, stopuri, parbriz și roți. Au culori variate dintr-o paletă de 8–10 culori. Modelul și culoarea sunt alese de server la intrare și se păstrează pe toată durata sesiunii (câmpuri noi în `ParkingSession`: `carModel`, `carColor`), ca mașina să arate la fel la fiecare reîncărcare și pe bilet.
- **Marcaje pe asfalt:** fiecare loc are numărul scris pe jos, între liniile albe, cu un font pixel mic (de exemplu `A01`).
- **3 locuri pentru persoane cu dizabilități:** marcate cu albastru și cu simbolul internațional (scaunul cu rotile) pe jos, în locul numărului. Le primesc doar mașinile simulate care au permis (cam 10% dintre ele, câmp `accessiblePermit`), iar celelalte nu parchează acolo. Pe bilet apare un mic simbol pentru permis. Cele 3 locuri sunt incluse în cele 24, aproape de intrare. *Detaliile sunt decizia lui Claude.*
- **Intrare și ieșire separate**, fiecare cu barieră proprie (dungi roșu-alb), lângă o cabină mică.

### Animațiile hărții (cerința lui Iosua: „să se vadă tot parcursul”)
- **Intrare:** mașina apare din afara hărții, se oprește la bariera de intrare, bariera se ridică animat, mașina trece și bariera coboară. Apoi merge pe culoar până în dreptul locului, virează și intră cu fața în loc, cu încetinire la final.
- **Ieșire:** mașina iese cu spatele din loc, virează și se îndreaptă spre ieșire, se oprește la barieră, bariera se ridică, mașina iese de pe hartă și bariera coboară.
- Mișcarea urmează un traseu de puncte pe culoar (linii și viraje rotunjite), iar mașina e rotită în direcția de mers. Vitezele sunt potrivite ca o intrare completă să dureze cam 4–6 secunde.
- Mai multe mașini se pot mișca în același timp. La barieră stau la coadă: nu trec una prin alta la barieră, dar pe culoar nu facem detecție completă de coliziuni. *Decizia lui Claude.*
- **Starea serverului rămâne adevărul:** locul e ocupat din momentul evenimentului `entered`, iar biletul se poate deschide imediat (chiar dacă mașina încă merge spre loc). La `snapshot` (încărcarea paginii sau reconectarea), mașinile apar direct pe locuri, fără animație.
- Cu `prefers-reduced-motion`, mașinile apar sau dispar direct pe loc, fără traseu.
- Desenarea folosește `requestAnimationFrame`, iar animația se oprește când tab-ul nu e vizibil. La revenire, se redesenează din starea curentă.
