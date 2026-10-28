<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%-- Pages shown by a servlet are forwards, so the address the visitor asked for is in this attribute --%>
<c:set var="path" value="${empty requestScope['jakarta.servlet.forward.request_uri'] ? pageContext.request.requestURI : requestScope['jakarta.servlet.forward.request_uri']}"/>
<header class="pp-top">
    <a class="pp-brand" href="${ctx}/" aria-label="ParkPulse, pagina principală">
        <t:logo/>
    </a>
    <nav aria-label="Meniu">
        <ul class="pp-nav">
            <li><a class="pp-pill ${path eq ctx.concat('/') or path.endsWith('/index.jsp') ? 'active' : ''}" href="${ctx}/">Live</a></li>
            <c:if test="${pageContext.request.isUserInRole('READ_CARS')}">
                <li><a class="pp-pill ${path.endsWith('/Cars') ? 'active' : ''}" href="${ctx}/Cars">Abonamente</a></li>
            </c:if>
            <c:if test="${pageContext.request.isUserInRole('READ_USERS')}">
                <li><a class="pp-pill ${path.endsWith('/Users') ? 'active' : ''}" href="${ctx}/Users">Useri</a></li>
            </c:if>
            <c:if test="${pageContext.request.isUserInRole('INVOICING')}">
                <li><a class="pp-pill ${path.endsWith('/billing') ? 'active' : ''}" href="${ctx}/billing">Încasări</a></li>
            </c:if>
            <li>
                <c:choose>
                    <c:when test="${pageContext.request.remoteUser == null}">
                        <a class="pp-pill pp-pill-primary" href="${ctx}/Account">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/></svg>
                            Autentificare
                        </a>
                    </c:when>
                    <c:otherwise>
                        <div class="dropdown">
                            <button class="pp-pill active dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/></svg>
                                <c:out value="${pageContext.request.remoteUser}"/>
                            </button>
                            <div class="dropdown-menu dropdown-menu-end">
                                <form method="POST" action="${ctx}/Logout">
                                    <t:csrf/>
                                    <button type="submit" class="dropdown-item">Logout</button>
                                </form>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </li>
        </ul>
    </nav>
</header>
