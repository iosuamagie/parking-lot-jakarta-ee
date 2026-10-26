# Parking lot (Jakarta EE)

A web app for managing the cars in a parking lot and the users who can see or change them. I built it step by step, one lab at a time, for a course at Lucian Blaga University of Sibiu (ULBS).

## Features

- Add, edit and delete cars, each with an owner, a license plate and a parking spot
- Upload a photo for a car and show it in the car list
- Form login backed by a JDBC realm, with passwords stored as SHA-256 hashes
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
2. On a Jakarta EE 10 server (Payara or GlassFish), create a JDBC resource named `jdbc/ParkingLotResource` and a JDBC realm named `jdbcRealmParkingLot` that reads usernames, SHA-256 passwords and groups from the app's tables. JPA creates the tables on first deploy.
3. Deploy `build/libs/*.war` and open the app in a browser.

## Known issues

The password hashes are not salted, so two users with the same password get the same hash. A salted algorithm such as PBKDF2 or bcrypt would be safer.
