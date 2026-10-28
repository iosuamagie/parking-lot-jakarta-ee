<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:pageTemplate pageTitle="Users · ParkPulse">
    <div class="pp-page">
        <div class="pp-page-head">
            <h1 class="pp-page-title">Users</h1>
            <c:if test="${pageContext.request.isUserInRole('WRITE_USERS')}">
                <div class="pp-actions">
                    <a class="btn btn-primary" href="${pageContext.request.contextPath}/AddUser">Add User</a>
                </div>
            </c:if>
        </div>

        <section class="pp-card">
            <div class="pp-table-wrap">
                <table class="pp-table">
                    <thead>
                    <tr>
                        <th>Username</th>
                        <th>Email</th>
                        <c:if test="${pageContext.request.isUserInRole('WRITE_USERS')}"><th></th></c:if>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="user" items="${users}">
                        <tr>
                            <td><c:out value="${user.username}"/></td>
                            <td><c:out value="${user.email}"/></td>
                            <c:if test="${pageContext.request.isUserInRole('WRITE_USERS')}">
                                <td class="pp-right">
                                    <a href="${pageContext.request.contextPath}/EditUser?id=${user.id}" class="btn btn-secondary btn-sm">Edit</a>
                                </td>
                            </c:if>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </section>
    </div>
</t:pageTemplate>
