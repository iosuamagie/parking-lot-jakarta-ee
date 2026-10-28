package org.example.parkinglot.live;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

/** Sends the live feed's heartbeat every 20 seconds. */
@Singleton
@Startup
public class LiveHeartbeat {

    @Inject
    LiveHub liveHub;

    @Schedule(hour = "*", minute = "*", second = "*/20", persistent = false)
    void beat() {
        liveHub.heartbeat();
    }
}
