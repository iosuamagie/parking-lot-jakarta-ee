<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:pageTemplate pageTitle="Add car photo">
    <h1>Add Car Photo</h1>

    <form class="needs-validation" novalidate method="POST" enctype="multipart/form-data"
          action="${pageContext.request.contextPath}/AddCarPhoto">
        <t:csrf/>

        <div class="row">
            <div class="col-md-6 mb-3">
                License plate: <c:out value="${car.licensePlate}"/>
            </div>
        </div>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label for="file">Photo (PNG, JPEG, GIF or WebP, at most 2 MB)</label>
                <input type="file" name="file" id="file" accept="image/png,image/jpeg,image/gif,image/webp" required>
                <div class="invalid-feedback">
                    Photo is required.
                </div>
            </div>
        </div>

        <input type="hidden" name="car_id" value="${car.id}"/>

        <hr class="mb-4">
        <button class="btn btn-primary btn-lg" type="submit">Save</button>
    </form>
</t:pageTemplate>
