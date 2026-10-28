<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pageTemplate pageTitle="Bilet ${ticketNumber} · ParkPulse" bodyClass="pp-page-ticket">
    <div class="pp-ticket-page" id="ticket" data-ticket-code="<c:out value='${ticketCode}'/>">
        <p class="pp-ticket-loading">Se încarcă biletul…</p>
    </div>
    <script type="module" src="${pageContext.request.contextPath}/scripts/ticket-page.js"></script>
</t:pageTemplate>
