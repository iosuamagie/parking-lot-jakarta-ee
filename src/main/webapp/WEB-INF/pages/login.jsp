<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<t:pageTemplate pageTitle="Sign in · ParkPulse">
    <div class="pp-login">
        <section class="pp-card pp-login-card">
            <svg class="pp-login-logo" width="44" height="44" viewBox="0 0 8 8" shape-rendering="crispEdges" aria-hidden="true">
                <rect width="8" height="8" fill="#4f46e5"/>
                <rect x="2" y="1" width="3" height="1" fill="#fff"/><rect x="2" y="2" width="1" height="5" fill="#fff"/>
                <rect x="5" y="2" width="1" height="2" fill="#fff"/><rect x="3" y="4" width="2" height="1" fill="#fff"/>
            </svg>

            <c:if test="${message != null}">
                <div class="alert alert-warning" role="alert">
                    <c:out value="${message}"/>
                </div>
            </c:if>

            <form method="POST" action="${pageContext.request.contextPath}/j_security_check">
                <h1 class="pp-page-title">Sign in</h1>
                <p class="pp-muted">Staff accounts only. The live lot is public.</p>
                <t:csrf/>
                <label for="username" class="form-label">Username</label>
                <input type="text" id="username" name="j_username" class="form-control" autocomplete="username" required autofocus/>
                <label for="password" class="form-label mt-3">Password</label>
                <input type="password" id="password" name="j_password" class="form-control" autocomplete="current-password" required/>
                <button class="btn btn-primary w-100 mt-4" type="submit">Sign in</button>
            </form>
        </section>
    </div>
</t:pageTemplate>
