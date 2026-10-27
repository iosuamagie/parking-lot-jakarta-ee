package org.example.parkinglot.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.parkinglot.parking.AcceleratedClock;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

/**
 * The one parking clock of the application. After a restart it continues from the latest time stored in the
 * database, so the history never has times from "the future".
 */
@Singleton
@Startup
public class ParkingClockBean {
    private static final Logger LOG = Logger.getLogger(ParkingClockBean.class.getName());
    private static final int DEFAULT_SPEED = 60;

    @PersistenceContext
    EntityManager entityManager;

    private AcceleratedClock clock;

    @PostConstruct
    void start() {
        Instant lastEntry = entityManager
                .createQuery("SELECT MAX(s.enteredAt) FROM ParkingSession s", Instant.class).getSingleResult();
        Instant lastExit = entityManager
                .createQuery("SELECT MAX(s.exitedAt) FROM ParkingSession s", Instant.class).getSingleResult();
        Instant anchor = latest(lastEntry, lastExit);
        Instant realNow = Instant.now();
        clock = new AcceleratedClock(anchor != null ? anchor : realNow, realNow, readSpeed(), Instant::now);
        LOG.info("Parking clock starts at " + clock.now() + ", " + clock.speed() + "x faster than real time");
    }

    private static Instant latest(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        return b == null || a.isAfter(b) ? a : b;
    }

    private static int readSpeed() {
        String value = System.getenv("PARKPULSE_CLOCK_SPEED");
        if (value == null || value.isBlank()) {
            return DEFAULT_SPEED;
        }
        try {
            int speed = Integer.parseInt(value.trim());
            if (speed >= 1) {
                return speed;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the warning below
        }
        LOG.warning("PARKPULSE_CLOCK_SPEED must be a whole number of at least 1, so the default "
                + DEFAULT_SPEED + " is used");
        return DEFAULT_SPEED;
    }

    /** Regular readings keep the gaps short, so only a sleeping computer makes a long one. */
    @Schedule(hour = "*", minute = "*", second = "*/5", persistent = false)
    @Lock(LockType.READ)
    void keepTime() {
        clock.now();
    }

    @Lock(LockType.READ)
    public Instant now() {
        // Milliseconds are enough, and every browser can read them
        return clock.now().truncatedTo(ChronoUnit.MILLIS);
    }

    @Lock(LockType.READ)
    public int speed() {
        return clock.speed();
    }
}
