<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<t:pageTemplate pageTitle="Edit User">
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger" role="alert">
            <c:out value="${errorMessage}"/>
        </div>
    </c:if>
    <h1>Edit User</h1>

    <form class="needs-validation" novalidate method="POST" action="${pageContext.request.contextPath}/EditUser">
        <t:csrf/>
        <input type="hidden" name="user_id" value="${user.id}"/>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label for="username">Username</label>
                <input type="text" class="form-control" id="username" name="username"
                       value="${fn:escapeXml(user.username)}" maxlength="100" required>
                <div class="invalid-feedback">
                    Username is required.
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label for="email">Email</label>
                <input type="email" class="form-control" id="email" name="email"
                       value="${fn:escapeXml(user.email)}" maxlength="100" required>
                <div class="invalid-feedback">
                    Email is required.
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label for="password">Password</label>
                <input type="password" class="form-control" id="password" name="password" minlength="8">
                <small class="form-text text-muted">
                    Leave empty if you don't want to change the password.
                </small>
            </div>
        </div>

        <div class="row">
            <div class="col-md-6 mb-3">
                <label for="user_groups">Groups</label>
                <select class="form-select d-block w-100" id="user_groups" name="user_groups" multiple>
                    <c:forEach var="group" items="${userGroups}">
                        <option value="${group}" ${user.groups.contains(group) ? 'selected' : ''}>${group}</option>
                    </c:forEach>
                </select>
            </div>
        </div>

        <hr class="mb-4">
        <button class="btn btn-primary btn-lg" type="submit">Save Changes</button>
    </form>
</t:pageTemplate>
