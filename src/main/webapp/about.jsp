<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pageTemplate pageTitle="Despre · ParkPulse">
    <div class="pp-page">
        <section class="pp-card pp-form">
            <h1 class="pp-page-title">Despre ParkPulse</h1>
            <p>O parcare publică simulată: mașini inventate intră și ies la câteva secunde, iar pagina principală le arată
                live, în pixel art. Fiecare mașină are un bilet cu cod QR și o chitanță PDF după ieșire.</p>
            <p>Proiectul e scris în Jakarta EE 10 (EJB, JPA, JAX-RS, JSP), rulează pe Payara Server cu PostgreSQL și
                are un API documentat la <a href="${pageContext.request.contextPath}/api-docs">/api-docs</a>.</p>
        </section>
    </div>
</t:pageTemplate>
