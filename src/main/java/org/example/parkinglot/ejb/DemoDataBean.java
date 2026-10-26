package org.example.parkinglot.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.example.parkinglot.common.Roles;

import java.util.List;
import java.util.logging.Logger;

/**
 * Fills an empty database with demo users and cars, so a fresh `docker compose up` is ready to try.
 * Runs only when PARKPULSE_DEMO_DATA=true; the demo password comes from PARKPULSE_DEMO_PASSWORD.
 */
@Singleton
@Startup
public class DemoDataBean {
    private static final Logger LOG = Logger.getLogger(DemoDataBean.class.getName());

    @Inject
    UserBean userBean;

    @Inject
    CarsBean carsBean;

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
        if (userBean.countUsers() > 0) {
            return;
        }

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
}
