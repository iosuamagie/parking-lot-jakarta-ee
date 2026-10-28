package org.example.parkinglot.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** The JSON API under /api. Payara describes it at /openapi, and /api-docs shows that with Swagger UI. */
@ApplicationPath("/api")
@OpenAPIDefinition(
        info = @Info(title = "ParkPulse API", version = "1.0", description = """
                API-ul parcării publice simulate ParkPulse. Parcarea, fluxul live și biletele sunt publice \
                (codul biletului e secret, ca un bilet de hârtie). Mașinile cer rolul READ_CARS, iar încasările \
                rolul INVOICING: autentifică-te întâi în aplicație, în același browser."""),
        // A relative address, so "Try it" calls the server the page was opened from
        servers = @Server(url = "/"),
        tags = {
                @Tag(name = "Parcare", description = "Starea parcării și fluxul live"),
                @Tag(name = "Bilete", description = "Biletul, codul QR și chitanța"),
                @Tag(name = "Mașini", description = "Mașinile înregistrate (rolul READ_CARS)"),
                @Tag(name = "Încasări", description = "Biletele plătite și raportul (rolul INVOICING)")
        })
public class ParkingLotApplication extends Application {
}
