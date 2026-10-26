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

## Stack

Java 21, Jakarta EE 10 (Servlets, JSP with JSTL, EJB, CDI, JPA, JAX-RS), Gradle.

```
src/main/java/org/example/parkinglot
  entities/   JPA entities: Car, CarPhoto, User, UserGroup
  ejb/        business logic and database access
  common/     DTOs passed to the pages and the API
  servlets/   controllers for cars, users and login
  rest/       JAX-RS API
src/main/webapp/WEB-INF/pages   JSP views
```

## Running it

1. Build the WAR file:
   ```
   ./gradlew war
   ```
2. On a Jakarta EE 10 server (Payara or GlassFish), create a JDBC resource named `jdbc/ParkingLotResource`. JPA creates the tables on first deploy. No security realm is needed: the login is set up in `SecurityConfig`.
3. Deploy `build/libs/*.war` and open the app in a browser.

Run the unit tests with `./gradlew test`.

Users created before the switch to PBKDF2 still have old SHA-256 hashes and can no longer log in; create them again.
