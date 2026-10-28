package org.example.parkinglot.live;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.example.parkinglot.common.LotSnapshotDto;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps the open live-page connections (server-sent events) and sends every change in the lot to all of them.
 */
@ApplicationScoped
public class LiveHub {
    private static final Logger LOG = Logger.getLogger(LiveHub.class.getName());
    /** Protects the server: each open page keeps one connection. */
    static final int MAX_SUBSCRIBERS = 200;

    public static final String SNAPSHOT = "snapshot";
    public static final String ENTERED = "entered";
    public static final String EXITED = "exited";
    public static final String STATS = "stats";

    private final Set<SseEventSink> subscribers = ConcurrentHashMap.newKeySet();
    private final Jsonb jsonb = JsonbBuilder.create();
    private volatile Sse sse;

    /**
     * Sends the snapshot to this new page only, then adds it to the pages that get every change.
     *
     * @return {@code false} when there are already too many open pages
     */
    public synchronized boolean subscribe(SseEventSink sink, Sse sse, LotSnapshotDto snapshot) {
        subscribers.removeIf(SseEventSink::isClosed);
        if (subscribers.size() >= MAX_SUBSCRIBERS) {
            return false;
        }
        this.sse = sse;
        send(sink, event(SNAPSHOT, snapshot));
        subscribers.add(sink);
        return true;
    }

    public synchronized void publish(String name, Object payload) {
        if (subscribers.isEmpty()) {
            return;
        }
        OutboundSseEvent event = event(name, payload);
        for (SseEventSink sink : subscribers) {
            send(sink, event);
        }
    }

    /**
     * A comment line that pages ignore. A closed page is only noticed when a write to it fails, so this
     * frees its place even when no car moves (and keeps proxies from closing a quiet connection).
     */
    public synchronized void heartbeat() {
        if (subscribers.isEmpty()) {
            return;
        }
        OutboundSseEvent ping = sse.newEventBuilder().comment("ping").build();
        for (SseEventSink sink : subscribers) {
            send(sink, ping);
        }
    }

    private OutboundSseEvent event(String name, Object payload) {
        return sse.newEventBuilder()
                .name(name)
                .mediaType(MediaType.TEXT_PLAIN_TYPE)
                .data(jsonb.toJson(payload))
                .build();
    }

    private void send(SseEventSink sink, OutboundSseEvent event) {
        if (sink.isClosed()) {
            subscribers.remove(sink);
            return;
        }
        sink.send(event).whenComplete((ignored, error) -> {
            if (error != null) {
                // The page was closed: forget it
                LOG.log(Level.FINE, "A live page went away", error);
                subscribers.remove(sink);
                sink.close();
            }
        });
    }
}
