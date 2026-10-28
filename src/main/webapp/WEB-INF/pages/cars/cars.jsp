<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:pageTemplate pageTitle="Abonamente · ParkPulse">
    <div class="pp-page">
        <form method="POST" action="${pageContext.request.contextPath}/Cars">
            <t:csrf/>
            <div class="pp-page-head">
                <h1 class="pp-page-title">Abonamente</h1>
                <span class="pp-muted">Locuri rezervate libere: ${numberOfFreeParkingSpots}</span>
                <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}">
                    <div class="pp-actions">
                        <c:choose>
                            <c:when test="${numberOfFreeParkingSpots > 0}">
                                <a class="btn btn-primary" href="${pageContext.request.contextPath}/AddCar">Adaugă abonament</a>
                            </c:when>
                            <c:otherwise>
                                <button class="btn btn-secondary" type="button" disabled>Toate locurile sunt rezervate</button>
                            </c:otherwise>
                        </c:choose>
                        <button class="btn btn-danger" type="submit">Șterge selectate</button>
                    </div>
                </c:if>
            </div>
            <p class="pp-muted pp-page-note">Mașinile cu abonament au un loc rezervat și un titular. Sunt separate de
                mașinile simulate de pe harta live.</p>

            <section class="pp-card">
                <div class="pp-table-wrap">
                    <table class="pp-table">
                        <thead>
                        <tr>
                            <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}"><th><span class="visually-hidden">Selectează</span></th></c:if>
                            <th>Număr</th>
                            <th>Loc</th>
                            <th>Titular</th>
                            <th>Poză</th>
                            <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}"><th></th></c:if>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="car" items="${cars}">
                            <tr>
                                <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}">
                                    <td>
                                        <input class="form-check-input" type="checkbox" name="car_ids" value="${car.id}"
                                               aria-label="Selectează <c:out value='${car.licensePlate}'/>"/>
                                    </td>
                                </c:if>
                                <td class="pp-plate-text"><c:out value="${car.licensePlate}"/></td>
                                <td><c:out value="${car.parkingSpot}"/></td>
                                <td><c:out value="${car.ownerName}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${carIdsWithPhoto.contains(car.id)}">
                                            <img class="pp-thumb" src="${pageContext.request.contextPath}/CarPhotos?id=${car.id}"
                                                 width="48" height="36" alt="Poza mașinii <c:out value='${car.licensePlate}'/>"/>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="pp-thumb pp-no-photo" title="Fără poză">
                                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                                     stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                                    <path d="M5 16l1.5-5a2 2 0 0 1 2-1.4h7a2 2 0 0 1 2 1.4L19 16"/>
                                                    <rect x="3" y="16" width="18" height="4" rx="1.5"/>
                                                    <circle cx="7" cy="20" r="1"/><circle cx="17" cy="20" r="1"/>
                                                </svg>
                                                <span class="visually-hidden">Fără poză</span>
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}">
                                    <td class="pp-right">
                                        <a class="btn btn-secondary btn-sm"
                                           href="${pageContext.request.contextPath}/AddCarPhoto?id=${car.id}">Adaugă poză</a>
                                        <a class="btn btn-secondary btn-sm"
                                           href="${pageContext.request.contextPath}/EditCar?id=${car.id}">Modifică</a>
                                    </td>
                                </c:if>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty cars}">
                            <tr><td colspan="6" class="pp-empty">Niciun abonament încă.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </form>
    </div>
</t:pageTemplate>
