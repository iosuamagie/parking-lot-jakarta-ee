<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:pageTemplate pageTitle="Cars">
    <h1>Cars</h1>

    <form method="POST" action="${pageContext.request.contextPath}/Cars">
        <t:csrf/>
        <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}">

            <c:choose>
                <c:when test="${numberOfFreeParkingSpots > 0}">
                    <a class="btn btn-primary btn-lg" href="${pageContext.request.contextPath}/AddCar">Add Car</a>
                </c:when>
                <c:otherwise>
                    <button class="btn btn-secondary btn-lg" type="button" disabled>
                        Parking Full
                    </button>
                </c:otherwise>
            </c:choose>
            <button class="btn btn-danger" type="submit">Delete Cars</button>

        </c:if>

        <div class="container text-center mt-3">
            <c:forEach var="car" items="${cars}">
                <div class="row align-items-center mb-2">
                    <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}">
                        <div class="col-1">
                            <input type="checkbox" name="car_ids" value="${car.id}"/>
                        </div>
                    </c:if>

                    <div class="col"><c:out value="${car.licensePlate}"/></div>
                    <div class="col"><c:out value="${car.parkingSpot}"/></div>
                    <div class="col"><c:out value="${car.ownerName}"/></div>

                    <div class="col">
                        <img src="${pageContext.request.contextPath}/CarPhotos?id=${car.id}" width="48" alt=""/>
                    </div>

                    <c:if test="${pageContext.request.isUserInRole('WRITE_CARS')}">
                        <div class="col">
                            <a class="btn btn-secondary"
                               href="${pageContext.request.contextPath}/AddCarPhoto?id=${car.id}"
                               role="button">Add photo</a>
                        </div>

                        <div class="col">
                            <a class="btn btn-secondary"
                               href="${pageContext.request.contextPath}/EditCar?id=${car.id}">Edit Car</a>
                        </div>
                    </c:if>
                </div>
            </c:forEach>
        </div>
    </form>
    <h5 class="mt-4">Free parking spots: ${numberOfFreeParkingSpots}</h5>
</t:pageTemplate>
