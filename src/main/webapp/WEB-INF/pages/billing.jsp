<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<t:pageTemplate pageTitle="Billing · ParkPulse" bodyClass="pp-page-billing">
    <div class="pp-billing">
        <div class="pp-billing-bar">
            <h1 class="pp-page-title">Billing</h1>
            <nav class="pp-segmented" aria-label="Period">
                <a class="pp-pill ${periodKey eq 'today' ? 'active' : ''}" href="${ctx}/billing?period=today">Today</a>
                <a class="pp-pill ${periodKey eq '7d' ? 'active' : ''}" href="${ctx}/billing?period=7d">Last 7 days</a>
                <a class="pp-pill ${periodKey eq 'month' ? 'active' : ''}" href="${ctx}/billing?period=month">This month</a>
            </nav>
            <form class="pp-custom-period" method="GET" action="${ctx}/billing">
                <input type="hidden" name="period" value="custom">
                <label>From <input type="date" name="from" value="<c:out value='${from}'/>" required></label>
                <label>to <input type="date" name="to" value="<c:out value='${to}'/>" required></label>
                <button type="submit" class="pp-pill ${periodKey eq 'custom' ? 'active' : ''}">Apply</button>
            </form>
            <a class="pp-pill pp-pill-primary pp-report" href="${ctx}/api/billing/report.pdf?${reportQuery}">⬇ PDF report for this period</a>
        </div>

        <div class="pp-billing-grid">
            <section class="pp-kpis" aria-label="Cifrele perioadei">
                <div class="pp-kpi">
                    <div class="pp-kpi-label">Earned in this period</div>
                    <div class="pp-kpi-value"><c:out value="${kpi.total}"/></div>
                    <c:if test="${not empty kpi.trend}">
                        <div class="pp-kpi-trend ${kpi.trendUp ? 'up' : 'down'}"><c:out value="${kpi.trend}"/></div>
                    </c:if>
                </div>
                <div class="pp-kpi">
                    <div class="pp-kpi-label">Paid tickets</div>
                    <div class="pp-kpi-value">${kpi.paid}</div>
                </div>
                <div class="pp-kpi">
                    <div class="pp-kpi-label">Average per ticket</div>
                    <div class="pp-kpi-value"><c:out value="${kpi.average}"/></div>
                </div>
                <div class="pp-kpi">
                    <div class="pp-kpi-label">Free (&lt;15 min)</div>
                    <div class="pp-kpi-value">${kpi.free}</div>
                </div>
            </section>

            <div class="pp-billing-main">
                <section class="pp-panel" aria-labelledby="chart-title">
                    <h2 id="chart-title">Money per day</h2>
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
                        <h2 id="tickets-title">Paid tickets <small class="pp-muted">(${total})</small></h2>
                        <form class="pp-search" method="GET" action="${ctx}/billing" role="search">
                            <input type="hidden" name="period" value="<c:out value='${periodKey}'/>">
                            <c:if test="${periodKey eq 'custom'}">
                                <input type="hidden" name="from" value="<c:out value='${from}'/>">
                                <input type="hidden" name="to" value="<c:out value='${to}'/>">
                            </c:if>
                            <input type="search" name="plate" value="<c:out value='${plate}'/>"
                                   placeholder="Search by plate…" aria-label="Search by plate">
                        </form>
                    </div>
                    <div class="pp-table-wrap">
                        <table class="pp-table">
                            <thead>
                            <tr><th>Ticket</th><th>Car</th><th>Spot</th><th>Entered → Left</th><th>Stay</th>
                                <th class="pp-right">Amount</th><th></th></tr>
                            </thead>
                            <tbody>
                            <c:forEach var="row" items="${rows}">
                                <tr>
                                    <td><a href="${ctx}/ticket/<c:out value='${row.code}'/>"><c:out value="${row.number}"/></a></td>
                                    <td class="pp-plate-text"><c:out value="${row.plate}"/></td>
                                    <td><c:out value="${row.spot}"/></td>
                                    <td><c:out value="${row.times}"/></td>
                                    <td><c:out value="${row.duration}"/></td>
                                    <td class="pp-right pp-amount ${row.free ? 'free' : ''}"><c:out value="${row.amount}"/></td>
                                    <td><a href="${ctx}/api/tickets/<c:out value='${row.code}'/>/receipt.pdf">PDF</a></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty rows}">
                                <tr><td colspan="7" class="pp-empty">No paid tickets in this period.</td></tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                    <nav class="pp-pager" aria-label="Pagini">
                        <c:choose>
                            <c:when test="${not empty prevQuery}"><a class="pp-pill" href="${ctx}/billing?${prevQuery}">‹ Previous</a></c:when>
                            <c:otherwise><span class="pp-pill disabled">‹ Previous</span></c:otherwise>
                        </c:choose>
                        <span>Page ${page} of ${pages}</span>
                        <c:choose>
                            <c:when test="${not empty nextQuery}"><a class="pp-pill" href="${ctx}/billing?${nextQuery}">Next ›</a></c:when>
                            <c:otherwise><span class="pp-pill disabled">Next ›</span></c:otherwise>
                        </c:choose>
                    </nav>
                </section>
            </div>
        </div>
    </div>
</t:pageTemplate>
