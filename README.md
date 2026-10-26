# Parking lot (Jakarta EE)

A web app for managing the cars in a parking lot and the users who can see or change them. I built it step by step, one lab at a time, for a course at Lucian Blaga University of Sibiu (ULBS).

## Features

- Add, edit and delete cars, each with an owner, a license plate and a parking spot
- Upload a photo for a car and show it in the car list
- Form login with Jakarta Security, configured in code; passwords are stored as salted PBKDF2 hashes
- CSRF tokens on every form, escaped output in every page, and photo uploads checked by their real file type
- Role-based access: `READ_CARS`, `WRITE_CARS`, `READ_USERS`, `WRITE_USERS` and `INVOICING`, checked on every servlet with `@ServletSecurity`
- Pick users for an invoice; the selection lives in a stateful, session-scoped EJB
- A small JSON API: `GET /resources/cars` and `GET /resources/cars/{id}`

## Run it in one command

You only need [Docker](https://www.docker.com/products/docker-desktop/).

```
docker compose up --build
```

The first start downloads Payara and PostgreSQL and takes a few minutes; later starts take about 30 seconds. Then open http://localhost:8080 and log in with one of the demo users, all with the password `parkpulse-demo`:

| User | What they can do |
|---|---|
| `admin` | everything |
| `viewer` | see cars and users |
| `billing` | see cars and users, pick users for an invoice |

If port 8080 is taken, pick another one: `PARKPULSE_PORT=8081 docker compose up --build`.

The demo passwords are for your own computer only. Before putting the app anywhere public, set `PARKPULSE_DB_PASSWORD` and `PARKPULSE_DEMO_PASSWORD` in a `.env` file (it is ignored by git), or turn the demo data off with `PARKPULSE_DEMO_DATA=false`.

To start again from an empty database: `docker compose down -v`.

## Stack

Java 21, Jakarta EE 10 (Servlets, JSP with JSTL, EJB, CDI, JPA, JAX-RS, Jakarta Security), Gradle, running on Payara Server 7 with PostgreSQL 17.

```
src/main/java/org/example/parkinglot
  entities/   JPA entities: Car, CarPhoto, User, UserGroup
  ejb/        business logic, database access, the data source and the demo data
  security/   login setup and the CSRF filter
  common/     DTOs passed to the pages and the API
  servlets/   controllers for cars, users and login
  rest/       JAX-RS API
src/main/webapp/WEB-INF/pages   JSP views
```

## Without Docker

1. Build the WAR file with `./gradlew war`.
2. Deploy `build/libs/parkpulse.war` on a Jakarta EE 10 or 11 server (tested on Payara 7) with these environment variables: `PARKPULSE_DB_URL` (for example `jdbc:postgresql://localhost:5432/parkpulse`), `PARKPULSE_DB_USER` and `PARKPULSE_DB_PASSWORD`. The PostgreSQL driver is inside the WAR, JPA creates the tables, and the login needs no server setup.

## Tests

- `./gradlew test` runs the unit tests (input checks, file type detection, CSRF token comparison). No Docker needed.
- `./gradlew integrationTest` starts the real app with [Testcontainers](https://testcontainers.com/): Payara and PostgreSQL in Docker, with the freshly built WAR. The tests then use it like a browser: log in as each demo user, try pages they are not allowed to see, send forms without a CSRF token, upload fake and oversized images, and check that user text is escaped.

GitHub Actions runs both on every push (`.github/workflows/ci.yml`).
