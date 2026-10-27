<%@tag description="base page template" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@attribute name="pageTitle"%>
<%@attribute name="bodyClass"%>
<!DOCTYPE html>
<html lang="ro">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${pageTitle}"/></title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous">
    <link href="${pageContext.request.contextPath}/styles/app.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js" integrity="sha384-FKyoEForCGlyvwx9Hj09JcYn3nv7wiPVlz7YYwJrWVcXK/BmnVDxM+D2scQbITxI" crossorigin="anonymous"></script>
</head>
<body class="<c:out value='${bodyClass}'/>" data-ctx="${pageContext.request.contextPath}">
    <jsp:include page="/WEB-INF/pages/menu.jsp"/>
    <main class="container-fluid">
        <jsp:doBody/>
    </main>
    <jsp:include page="/WEB-INF/pages/footer.jsp"/>
    <script src="${pageContext.request.contextPath}/scripts/form_validation.js"></script>
</body>
</html>
