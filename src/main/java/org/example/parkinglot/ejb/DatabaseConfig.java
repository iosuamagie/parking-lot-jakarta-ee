package org.example.parkinglot.ejb;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Singleton;

/**
 * The database connection, defined in code so the server needs no JDBC setup.
 * Payara fills in the ${ENV=...} values from environment variables (see docker-compose.yml).
 */
@DataSourceDefinition(
        name = DatabaseConfig.DATA_SOURCE,
        className = "org.postgresql.ds.PGSimpleDataSource",
        url = "${ENV=PARKPULSE_DB_URL}",
        user = "${ENV=PARKPULSE_DB_USER}",
        password = "${ENV=PARKPULSE_DB_PASSWORD}",
        minPoolSize = 2,
        maxPoolSize = 20)
@Singleton
public class DatabaseConfig {
    public static final String DATA_SOURCE = "java:app/jdbc/parkpulse";
}
