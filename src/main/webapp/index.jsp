<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pageTemplate pageTitle="ParkPulse · parcarea live" bodyClass="pp-page-live">
    <div class="pp-live-grid" id="live">

        <section class="pp-kpis" aria-label="Cifrele zilei">
            <div class="pp-kpi">
                <div class="pp-kpi-label">Free spots</div>
                <div class="pp-kpi-value"><span id="kpi-free">–</span><small> / <span id="kpi-capacity">24</span></small></div>
                <div class="pp-meter"><div id="kpi-free-bar"></div></div>
            </div>
            <div class="pp-kpi">
                <div class="pp-kpi-label">Earned today</div>
                <div class="pp-kpi-value" id="kpi-revenue">–</div>
                <div class="pp-kpi-trend" id="kpi-trend"></div>
            </div>
            <div class="pp-kpi">
                <div class="pp-kpi-label">Cars today</div>
                <div class="pp-kpi-value" id="kpi-cars">–</div>
            </div>
            <div class="pp-kpi">
                <div class="pp-kpi-label">Average stay</div>
                <div class="pp-kpi-value" id="kpi-stay">–</div>
            </div>
        </section>

        <section class="pp-panel" aria-labelledby="lot-title">
            <div class="pp-panel-head">
                <h2 id="lot-title">The lot now</h2>
                <span class="pp-clock" id="clock">Parking time --:--</span>
                <button type="button" class="pp-pill pp-motion-toggle" id="motion-toggle" aria-pressed="true">Animations on</button>
                <span class="pp-live-badge off" id="live-badge"><span class="pp-dot"></span>Connecting…</span>
            </div>
            <canvas id="lot" width="320" height="128" role="img"
                    aria-label="Map of the parking lot with the cars parked now"></canvas>
            <p class="pp-hint">Click a car to see its ticket. The parking clock runs 60 times faster than real time.</p>
        </section>

        <section class="pp-panel" aria-labelledby="activity-title">
            <h2 id="activity-title">Activity</h2>
            <ol id="activity" class="pp-activity"></ol>
        </section>
    </div>
    <script type="module" src="${pageContext.request.contextPath}/scripts/live.js"></script>
</t:pageTemplate>
