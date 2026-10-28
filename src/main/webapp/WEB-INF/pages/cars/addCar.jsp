<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:pageTemplate pageTitle="Add Car">
    <div class="pp-page">
        <section class="pp-card pp-form">
            <c:if test="${not empty errorMessage}">
                <div class="alert alert-danger" role="alert">
                    <c:out value="${errorMessage}"/>
                </div>
            </c:if>
            <h1 class="pp-page-title">Add Car</h1>

            <form class="needs-validation" novalidate method="POST" action="${pageContext.request.contextPath}/AddCar">
                <t:csrf/>
                <div class="row g-3">
                    <div class="col-md-6 mb-3">
                        <label for="license_plate" class="form-label">License Plate</label>
                        <input type="text" class="form-control" id="license_plate" name="license_plate" minlength="3" maxlength="100" required>
                        <div class="invalid-feedback">
                            License Plate is required.
                        </div>
                    </div>
                    <div class="col-md-6 mb-3">
                        <label for="parking_spot" class="form-label">Parking Spot</label>
                        <input type="text" class="form-control" id="parking_spot" name="parking_spot" minlength="3" maxlength="100" required>
                        <div class="invalid-feedback">
                            Parking Spot is required.
                        </div>
                    </div>
                    <div class="col-12 mb-3">
                        <label for="owner_id" class="form-label">Owner</label>
                        <select class="form-select" id="owner_id" name="owner_id" required>
                            <option value="">Choose...</option>
                            <c:forEach var="user" items="${users}">
                                <option value="${user.id}"><c:out value="${user.username}"/></option>
                            </c:forEach>
                        </select>
                        <div class="invalid-feedback">
                            Please select an owner.
                        </div>
                    </div>
                </div>
                <hr class="my-4">
                <button class="w-100 btn btn-primary btn-lg" type="submit">Save</button>
            </form>
        </section>
    </div>
</t:pageTemplate>
