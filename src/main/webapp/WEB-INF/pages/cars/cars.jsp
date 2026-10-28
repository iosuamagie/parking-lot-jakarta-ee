<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:pageTemplate pageTitle="Cars · ParkPulse">
    <div class="pp-page">
        <form method="POST" action="${pageContext.request.contextPath}/Cars">
            <t:csrf/>
            <div class="pp-page-head">
                <h1 class="pp-page-title">Cars</h1>
                <span class="pp-muted">Free parking spots: ${numberOfFreeParkingSpots}</span>
                <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}">
                    <div class="pp-actions">
                        <c:choose>
                            <c:when test="${numberOfFreeParkingSpots > 0}">
                                <a class="btn btn-primary" href="${pageContext.request.contextPath}/AddCar">Add Car</a>
                            </c:when>
                            <c:otherwise>
                                <button class="btn btn-secondary" type="button" disabled>Parking Full</button>
                            </c:otherwise>
                        </c:choose>
                        <button class="btn btn-danger" type="submit">Delete Cars</button>
                    </div>
                </c:if>
            </div>

            <section class="pp-card">
                <div class="pp-table-wrap">
                    <table class="pp-table">
                        <thead>
                        <tr>
                            <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}"><th><span class="visually-hidden">Select</span></th></c:if>
                            <th>License plate</th>
                            <th>Spot</th>
                            <th>Owner</th>
                            <th>Photo</th>
                            <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}"><th></th></c:if>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="car" items="${cars}">
                            <tr>
                                <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}">
                                    <td>
                                        <input class="form-check-input" type="checkbox" name="car_ids" value="${car.id}"
                                               aria-label="Select <c:out value='${car.licensePlate}'/>"/>
                                    </td>
                                </c:if>
                                <td class="pp-plate-text"><c:out value="${car.licensePlate}"/></td>
                                <td><c:out value="${car.parkingSpot}"/></td>
                                <td><c:out value="${car.ownerName}"/></td>
                                <td>
                                    <img class="pp-thumb" src="${pageContext.request.contextPath}/CarPhotos?id=${car.id}" width="48" height="36" alt=""/>
                                </td>
                                <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}">
                                    <td class="pp-right">
                                        <a class="btn btn-secondary btn-sm"
                                           href="${pageContext.request.contextPath}/AddCarPhoto?id=${car.id}">Add photo</a>
                                        <a class="btn btn-secondary btn-sm"
                                           href="${pageContext.request.contextPath}/EditCar?id=${car.id}">Edit Car</a>
                                    </td>
                                </c:if>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty cars}">
                            <tr><td colspan="6" class="pp-empty">No cars yet.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </form>
    </div>
</t:pageTemplate>
