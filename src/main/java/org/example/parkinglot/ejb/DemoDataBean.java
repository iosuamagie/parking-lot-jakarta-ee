package org.example.parkinglot.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.example.parkinglot.common.Roles;
import org.example.parkinglot.entities.ParkingSession;
import org.example.parkinglot.parking.AcceleratedClock;
import org.example.parkinglot.parking.CarLooks;
import org.example.parkinglot.parking.CarModel;
import org.example.parkinglot.parking.PlateGenerator;
import org.example.parkinglot.parking.SimulatorPolicy;
import org.example.parkinglot.parking.Spots;
import org.example.parkinglot.parking.Tariff;
import org.example.parkinglot.parking.TicketCodes;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

/**
 * Fills an empty database with demo users and cars, so a fresh `docker compose up` is ready to try.
 * It also adds 30 days of parking history and a dozen parked cars, so the live page and the billing page
 * have something to show right away.
 * Runs only when PARKPULSE_DEMO_DATA=true; the demo password comes from PARKPULSE_DEMO_PASSWORD.
 */
@Singleton
@Startup
@DependsOn("ParkingClockBean")
public class DemoDataBean {
    private static final Logger LOG = Logger.getLogger(DemoDataBean.class.getName());
    private static final int HISTORY_DAYS = 30;
    private static final int OPEN_SESSIONS = 12;

    @Inject
    UserBean userBean;

    @Inject
    CarsBean carsBean;

    @Inject
    ParkingSessionBean parkingSessionBean;

    @Inject
    ParkingClockBean clock;

    @PostConstruct
    void createDemoData() {
        if (!"true".equalsIgnoreCase(System.getenv("PARKPULSE_DEMO_DATA"))) {
            return;
        }
        String password = System.getenv("PARKPULSE_DEMO_PASSWORD");
        if (password == null || password.length() < 8) {
            LOG.warning("PARKPULSE_DEMO_PASSWORD is missing or shorter than 8 characters, so no demo data was created");
            return;
        }
        if (userBean.countUsers() == 0) {
            createUsersAndCars(password);
        }
        if (parkingSessionBean.countSessions() == 0) {
            createParkingHistory();
        }
    }

    private void createUsersAndCars(String password) {
        LOG.info("Empty database: creating the demo users and cars");
        Long admin = userBean.createUser("admin", "admin@parkpulse.local", password, Roles.ALL);
        Long viewer = userBean.createUser("viewer", "viewer@parkpulse.local", password,
                List.of(Roles.READ_CARS, Roles.READ_USERS));
        Long billing = userBean.createUser("billing", "billing@parkpulse.local", password,
                List.of(Roles.READ_CARS, Roles.READ_USERS, Roles.INVOICING));

        carsBean.createCar("SB-01-PKP", "A01", admin);
        carsBean.createCar("SB-22-ULB", "A02", viewer);
        carsBean.createCar("B-404-JEE", "A03", billing);
        carsBean.createCar("CJ-10-EJB", "B01", viewer);
    }

    /** Closed tickets for the last 30 days (and today so far), plus the cars parked right now. */
    private void createParkingHistory() {
        RandomGenerator random = new SplittableRandom();
        Instant now = clock.now();
        LocalDate today = now.atZone(AcceleratedClock.ZONE).toLocalDate();
        List<ParkingSession> history = new ArrayList<>();

        for (LocalDate day = today.minusDays(HISTORY_DAYS); !day.isAfter(today); day = day.plusDays(1)) {
            Instant opening = day.atTime(6, 0).atZone(AcceleratedClock.ZONE).toInstant();
            int tickets = random.nextInt(90, 141);
            for (int i = 0; i < tickets; i++) {
                Instant entered = opening.plus(Duration.ofMinutes(random.nextInt(0, 17 * 60)));
                Duration stay = Duration.ofMinutes(random.nextInt(5, 241));
                Instant exited = entered.plus(stay);
                if (exited.isAfter(now)) {
                    continue;
                }
                ParkingSession session = randomSession(random, PlateGenerator.next(random, Set.of()),
                        randomRegularSpot(random), false, entered);
                session.setExitedAt(exited);
                session.setAmountBani(Tariff.amountBani(stay));
                history.add(session);
            }
        }

        Set<String> freeSpots = new HashSet<>(Spots.ALL);
        Set<String> plates = new HashSet<>();
        for (int i = 0; i < OPEN_SESSIONS; i++) {
            boolean permit = SimulatorPolicy.drawPermit(random);
            String spot = SimulatorPolicy.chooseSpot(freeSpots, permit, random).orElseThrow();
            freeSpots.remove(spot);
            String plate = PlateGenerator.next(random, plates);
            plates.add(plate);
            Instant entered = now.minus(Duration.ofMinutes(random.nextInt(1, 181)));
            history.add(randomSession(random, plate, spot, permit, entered));
        }

        parkingSessionBean.importHistory(history);
        LOG.info("Created " + history.size() + " demo parking sessions");
    }

    private static String randomRegularSpot(RandomGenerator random) {
        return Spots.ALL.get(random.nextInt(Spots.ACCESSIBLE.size(), Spots.CAPACITY));
    }

    private static ParkingSession randomSession(RandomGenerator random, String plate, String spot, boolean permit,
                                                Instant entered) {
        CarModel model = CarLooks.randomModel(random);
        ParkingSession session = new ParkingSession();
        session.setTicketCode(TicketCodes.newCode());
        session.setLicensePlate(plate);
        session.setSpot(spot);
        session.setCarModel(model);
        session.setCarColor(CarLooks.randomColor(random, model));
        session.setAccessiblePermit(permit);
        session.setEnteredAt(entered);
        return session;
    }
}
