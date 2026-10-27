package org.example.parkinglot.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import org.example.parkinglot.common.ActivityDto;
import org.example.parkinglot.common.ParkedCarDto;
import org.example.parkinglot.parking.CarLooks;
import org.example.parkinglot.parking.CarModel;
import org.example.parkinglot.parking.PlateGenerator;
import org.example.parkinglot.parking.SimulatorPolicy;
import org.example.parkinglot.parking.Spots;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Lets random cars enter and leave the public lot every few seconds.
 * Runs only when PARKPULSE_SIMULATOR=true.
 */
@Singleton
@Startup
@DependsOn({"ParkingClockBean", "DemoDataBean"})
public class SimulatorBean {
    private static final Logger LOG = Logger.getLogger(SimulatorBean.class.getName());

    @Inject
    ParkingSessionBean sessions;

    @Inject
    ParkingClockBean clock;

    private final RandomGenerator random = new SplittableRandom();
    private boolean enabled;
    /** Real time of the next car movement. */
    private Instant nextMoveAt;

    @PostConstruct
    void start() {
        enabled = "true".equalsIgnoreCase(System.getenv("PARKPULSE_SIMULATOR"));
        nextMoveAt = Instant.now();
        LOG.info(enabled ? "The traffic simulator is on" : "The traffic simulator is off (PARKPULSE_SIMULATOR)");
    }

    /**
     * Runs every second but moves a car only when its time has come. Each enter or exit commits on its own
     * (no transaction here), so the database already has the change when the live page hears about it.
     */
    @Schedule(hour = "*", minute = "*", second = "*", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void tick() {
        Instant realNow = Instant.now();
        if (!enabled || realNow.isBefore(nextMoveAt)) {
            return;
        }
        nextMoveAt = realNow.plus(SimulatorPolicy.nextDelay(random));
        try {
            moveOneCar();
        } catch (Exception ex) {
            // A failed move must not stop the timer; the next tick simply tries again
            LOG.log(Level.WARNING, "The simulator could not move a car", ex);
        }
    }

    private void moveOneCar() {
        List<ParkedCarDto> parked = sessions.findParked();
        SimulatorPolicy.Action action = SimulatorPolicy.decide(parked.size(), Spots.CAPACITY, random);
        if (action == SimulatorPolicy.Action.ENTER) {
            Optional<ParkedCarDto> entered = letACarIn(parked);
            if (entered.isPresent()) {
                LOG.fine(() -> "Entered: " + entered.get());
                return;
            }
            // No spot fits this car, so a car leaves instead
        }
        if (action == SimulatorPolicy.Action.NOTHING || parked.isEmpty()) {
            return;
        }
        String spot = SimulatorPolicy.pickLeaving(parked, clock.now(), random);
        ActivityDto exited = sessions.exit(spot);
        LOG.fine(() -> "Exited: " + exited);
    }

    private Optional<ParkedCarDto> letACarIn(List<ParkedCarDto> parked) {
        Set<String> free = new HashSet<>(Spots.ALL);
        parked.forEach(car -> free.remove(car.spot()));
        boolean permit = SimulatorPolicy.drawPermit(random);
        Optional<String> spot = SimulatorPolicy.chooseSpot(free, permit, random);
        if (spot.isEmpty()) {
            return Optional.empty();
        }
        Set<String> platesInside = parked.stream().map(ParkedCarDto::plate).collect(Collectors.toSet());
        CarModel model = CarLooks.randomModel(random);
        return Optional.of(sessions.enter(PlateGenerator.next(random, platesInside), spot.get(), model,
                CarLooks.randomColor(random, model), permit));
    }
}
