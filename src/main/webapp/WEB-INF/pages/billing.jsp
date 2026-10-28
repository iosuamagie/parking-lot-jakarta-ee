<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:pageTemplate pageTitle="Încasări · ParkPulse" bodyClass="pp-page-billing">
    <div class="pp-billing">
        <div class="pp-billing-bar">
            <h1 class="pp-page-title">Încasări</h1>
            <nav class="pp-segmented" aria-label="Perioada">
                <a class="pp-pill ${periodKey eq 'today' ? 'active' : ''}" href="${ctx}/billing?period=today">Azi</a>
                <a class="pp-pill ${periodKey eq '7d' ? 'active' : ''}" href="${ctx}/billing?period=7d">Ultimele 7 zile</a>
                <a class="pp-pill ${periodKey eq 'month' ? 'active' : ''}" href="${ctx}/billing?period=month">Luna asta</a>
            </nav>
            <form class="pp-custom-period" method="GET" action="${ctx}/billing">
                <input type="hidden" name="period" value="custom">
                <label>De la <input type="date" name="from" value="<c:out value='${from}'/>" required></label>
                <label>până la <input type="date" name="to" value="<c:out value='${to}'/>" required></label>
                <button type="submit" class="pp-pill ${periodKey eq 'custom' ? 'active' : ''}">Alege perioada</button>
            </form>
            <a class="pp-pill pp-pill-primary pp-report" href="${ctx}/api/billing/report.pdf?${reportQuery}">⬇ Raport PDF pe perioadă</a>
        </div>

        <div class="pp-billing-grid">
            <section class="pp-kpis" aria-label="Cifrele perioadei">
                <div class="pp-kpi">
                    <div class="pp-kpi-label">Încasat în perioadă</div>
                    <div class="pp-kpi-value"><c:out value="${kpi.total}"/></div>
                    <c:if test="${not empty kpi.trend}">
                        <div class="pp-kpi-trend ${kpi.trendUp ? 'up' : 'down'}"><c:out value="${kpi.trend}"/></div>
                    </c:if>
                </div>
                <div class="pp-kpi">
                    <div class="pp-kpi-label">Bilete plătite</div>
                    <div class="pp-kpi-value">${kpi.paid}</div>
                </div>
                <div class="pp-kpi">
                    <div class="pp-kpi-label">Medie pe bilet</div>
                    <div class="pp-kpi-value"><c:out value="${kpi.average}"/></div>
                </div>
                <div class="pp-kpi">
                    <div class="pp-kpi-label">Gratuite (&lt;15 min)</div>
                    <div class="pp-kpi-value">${kpi.free}</div>
                </div>
            </section>

            <div class="pp-billing-main">
                <section class="pp-panel" aria-labelledby="chart-title">
                    <h2 id="chart-title">Încasări pe zile</h2>
                    <div class="pp-chart">
                        <c:forEach var="bar" items="${bars}" varStatus="status">
                            <div class="pp-chart-col" title="<c:out value='${bar.title}'/>">
                                <div class="pp-chart-track">
                                    <div class="pp-bar ${bar.today ? 'today' : ''}"
                                         style="height:${bar.height}%;animation-delay:${status.index < 40 ? status.index * 40 : 0}ms"></div>
                                </div>
                                <span class="pp-chart-label"><c:out value="${bar.label}"/></span>
                            </div>
                        </c:forEach>
                    </div>
                </section>

                <section class="pp-panel" aria-labelledby="tickets-title">
                    <div class="pp-panel-head">
                        <h2 id="tickets-title">Bilete plătite <small class="pp-muted">(${total})</small></h2>
                        <form class="pp-search" method="GET" action="${ctx}/billing" role="search">
                            <input type="hidden" name="period" value="<c:out value='${periodKey}'/>">
                            <c:if test="${periodKey eq 'custom'}">
                                <input type="hidden" name="from" value="<c:out value='${from}'/>">
                                <input type="hidden" name="to" value="<c:out value='${to}'/>">
                            </c:if>
                            <input type="search" name="plate" value="<c:out value='${plate}'/>"
                                   placeholder="Caută după număr…" aria-label="Caută după număr">
                        </form>
                    </div>
                    <div class="pp-table-wrap">
                        <table class="pp-table">
                            <thead>
                            <tr><th>Bilet</th><th>Mașină</th><th>Loc</th><th>Intrare → Ieșire</th><th>Durată</th>
                                <th class="pp-right">Sumă</th><th></th></tr>
                            </thead>
                            <tbody>
                            <c:forEach var="row" items="${rows}">
                                <tr>
                                    <td><a href="${ctx}/bilet/<c:out value='${row.code}'/>"><c:out value="${row.number}"/></a></td>
                                    <td class="pp-plate-text"><c:out value="${row.plate}"/></td>
                                    <td><c:out value="${row.spot}"/></td>
                                    <td><c:out value="${row.times}"/></td>
                                    <td><c:out value="${row.duration}"/></td>
                                    <td class="pp-right pp-amount ${row.free ? 'free' : ''}"><c:out value="${row.amount}"/></td>
                                    <td><a href="${ctx}/api/tickets/<c:out value='${row.code}'/>/receipt.pdf">PDF</a></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty rows}">
                                <tr><td colspan="7" class="pp-empty">Niciun bilet plătit în perioada asta.</td></tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                    <nav class="pp-pager" aria-label="Pagini">
                        <c:choose>
                            <c:when test="${not empty prevQuery}"><a class="pp-pill" href="${ctx}/billing?${prevQuery}">‹ Înapoi</a></c:when>
                            <c:otherwise><span class="pp-pill disabled">‹ Înapoi</span></c:otherwise>
                        </c:choose>
                        <span>Pagina ${page} din ${pages}</span>
                        <c:choose>
                            <c:when test="${not empty nextQuery}"><a class="pp-pill" href="${ctx}/billing?${nextQuery}">Înainte ›</a></c:when>
                            <c:otherwise><span class="pp-pill disabled">Înainte ›</span></c:otherwise>
                        </c:choose>
                    </nav>
                </section>
            </div>
        </div>
    </div>
</t:pageTemplate>
