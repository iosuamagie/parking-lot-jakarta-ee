package org.example.parkinglot.it;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * The app as it runs in docker-compose.yml: Payara Server with the built WAR, plus PostgreSQL.
 * Started once and shared by all integration tests.
 */
final class ParkPulseServer {
    // Same images as docker-compose.yml and the Dockerfile
    private static final String PAYARA_IMAGE = "payara/server-full:7.2026.9";
    private static final String POSTGRES_IMAGE = "postgres:17.11-alpine";

    static final String DEMO_PASSWORD = "integration-demo";

    private static GenericContainer<?> payara;

    private ParkPulseServer() {
    }

    static synchronized String baseUrl() {
        if (payara == null) {
            start();
        }
        return "http://" + payara.getHost() + ":" + payara.getMappedPort(8080);
    }

    private static void start() {
        Path war = Path.of(System.getProperty("parkpulse.war", "build/libs/parkpulse.war"));
        if (!Files.exists(war)) {
            throw new IllegalStateException("Build the WAR first (./gradlew war), missing: " + war);
        }

        Network network = Network.newNetwork();

        PostgreSQLContainer database = new PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE))
                .withNetwork(network)
                .withNetworkAliases("db")
                .withDatabaseName("parkpulse")
                .withUsername("parkpulse")
                .withPassword("integration-db");
        database.start();

        payara = new GenericContainer<>(DockerImageName.parse(PAYARA_IMAGE))
                .withNetwork(network)
                .withEnv("PARKPULSE_DB_URL", "jdbc:postgresql://db:5432/parkpulse")
                .withEnv("PARKPULSE_DB_USER", "parkpulse")
                .withEnv("PARKPULSE_DB_PASSWORD", "integration-db")
                .withEnv("PARKPULSE_DEMO_DATA", "true")
                .withEnv("PARKPULSE_DEMO_PASSWORD", DEMO_PASSWORD)
                .withEnv("PARKPULSE_SIMULATOR", "true")
                .withCopyFileToContainer(MountableFile.forHostPath(war), "/opt/payara/deployments/parkpulse.war")
                .withExposedPorts(8080)
                // Payara says it started before the app is deployed, so wait for a real page
                .waitingFor(Wait.forHttp("/api/lot").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));
        payara.start();
        // Testcontainers stops both containers when the test JVM exits
    }
}
