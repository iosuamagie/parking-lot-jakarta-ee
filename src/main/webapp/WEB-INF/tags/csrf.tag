<%@tag description="hidden CSRF token for POST forms, checked by CsrfFilter" pageEncoding="UTF-8"%>
<input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"/>
