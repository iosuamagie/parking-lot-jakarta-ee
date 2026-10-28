<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pageTemplate pageTitle="About · ParkPulse">
    <div class="pp-page">
        <section class="pp-card pp-form">
            <h1 class="pp-page-title">About ParkPulse</h1>
            <p>A simulated public parking lot: made-up cars come and go every few seconds, and the home page shows them
                live, in pixel art. Every car gets a ticket with a QR code and a PDF receipt after it leaves.</p>
            <p>It is written in Jakarta EE 10 (EJB, JPA, JAX-RS, JSP), runs on Payara Server with PostgreSQL and
                has an API documented at <a href="${pageContext.request.contextPath}/api-docs">/api-docs</a>.</p>
        </section>
    </div>
</t:pageTemplate>
