<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<t:pageTemplate pageTitle="Edit subscription · ParkPulse">
    <div class="pp-page">
        <section class="pp-card pp-form">
            <c:if test="${not empty errorMessage}">
                <div class="alert alert-danger" role="alert">
                    <c:out value="${errorMessage}"/>
                </div>
            </c:if>

            <h1 class="pp-page-title">Edit subscription</h1>

            <form class="needs-validation" novalidate method="POST" action="${pageContext.request.contextPath}/EditCar">
                <t:csrf/>
                <div class="row g-3">
                    <div class="col-md-6 mb-3">
                        <label for="license_plate" class="form-label">License plate</label>
                        <input type="text" class="form-control" id="license_plate" name="license_plate"
                               value="${fn:escapeXml(car.licensePlate)}" minlength="3" maxlength="100" required>
                        <div class="invalid-feedback">
                            Enter the license plate.
                        </div>
                    </div>

                    <div class="col-md-6 mb-3">
                        <label for="parking_spot" class="form-label">Reserved spot</label>
                        <input type="text" class="form-control" id="parking_spot" name="parking_spot"
                               value="${fn:escapeXml(car.parkingSpot)}" minlength="3" maxlength="100" required>
                        <div class="invalid-feedback">
                            Scrie locul rezervat.
                        </div>
                    </div>

                    <div class="col-12 mb-3">
                        <label for="owner_id" class="form-label">Holder</label>
                        <select class="form-select" id="owner_id" name="owner_id" required>
                            <option value="">Choose...</option>
                            <c:forEach var="user" items="${users}">
                                <option value="${user.id}" ${car.ownerName eq user.username ? 'selected' : ''}>
                                    <c:out value="${user.username}"/>
                                </option>
                            </c:forEach>
                        </select>
                        <div class="invalid-feedback">
                            Choose who holds the subscription.
                        </div>
                    </div>
                </div>

                <hr class="mb-4">
                <input type="hidden" name="car_id" value="${car.id}"/>
                <button class="w-100 btn btn-primary btn-lg" type="submit">Save changes</button>
            </form>

        </section>
    </div>
</t:pageTemplate>
