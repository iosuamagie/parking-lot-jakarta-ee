<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="swagger" value="${pageContext.request.contextPath}/webjars/swagger-ui/5.33.2"/>
<t:pageTemplate pageTitle="API · ParkPulse" bodyClass="pp-page-api">
    <link rel="stylesheet" href="${swagger}/swagger-ui.css">
    <div class="pp-api-docs">
        <section class="pp-panel">
            <div id="swagger"><p class="pp-muted">Loading the documentation…</p></div>
        </section>
    </div>
    <script src="${swagger}/swagger-ui-bundle.js"></script>
    <script src="${pageContext.request.contextPath}/scripts/api-docs.js"></script>
</t:pageTemplate>
