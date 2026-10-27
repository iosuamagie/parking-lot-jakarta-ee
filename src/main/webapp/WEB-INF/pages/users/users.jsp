<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:pageTemplate pageTitle="Users">
    <div>
        <h1>Users</h1>

        <c:if test="${pageContext.request.isUserInRole('WRITE_USERS')}">
            <a class="btn btn-primary btn-lg" href="${pageContext.request.contextPath}/AddUser">Add User</a>
        </c:if>

        <div class="container text-center mt-3">
            <div class="row fw-bold border-bottom pb-2">
                <div class="col">
                    Username
                </div>
                <div class="col">
                    Email
                </div>
                <c:if test="${pageContext.request.isUserInRole('WRITE_USERS')}">
                    <div class="col-2"></div>
                </c:if>
            </div>

            <c:forEach var="user" items="${users}">
                <div class="row border-bottom py-2 align-items-center">

                    <div class="col">
                        <c:out value="${user.username}"/>
                    </div>
                    <div class="col">
                        <c:out value="${user.email}"/>
                    </div>

                    <c:if test="${pageContext.request.isUserInRole('WRITE_USERS')}">
                        <div class="col-2">
                            <a href="${pageContext.request.contextPath}/EditUser?id=${user.id}" class="btn btn-secondary">Edit</a>
                        </div>
                    </c:if>
                </div>
            </c:forEach>
        </div>
    </div>
</t:pageTemplate>
