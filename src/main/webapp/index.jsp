<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:pageTemplate pageTitle="ParkPulse · parcarea live" bodyClass="pp-page-live">
    <div class="pp-live-grid" id="live" data-ctx="${pageContext.request.contextPath}">

        <section class="pp-kpis" aria-label="Cifrele zilei">
            <div class="pp-kpi">
                <div class="pp-kpi-label">Locuri libere</div>
                <div class="pp-kpi-value"><span id="kpi-free">–</span><small> / <span id="kpi-capacity">24</span></small></div>
                <div class="pp-meter"><div id="kpi-free-bar"></div></div>
            </div>
            <div class="pp-kpi">
                <div class="pp-kpi-label">Încasat azi</div>
                <div class="pp-kpi-value" id="kpi-revenue">–</div>
                <div class="pp-kpi-trend" id="kpi-trend"></div>
            </div>
            <div class="pp-kpi">
                <div class="pp-kpi-label">Mașini azi</div>
                <div class="pp-kpi-value" id="kpi-cars">–</div>
            </div>
            <div class="pp-kpi">
                <div class="pp-kpi-label">Durata medie</div>
                <div class="pp-kpi-value" id="kpi-stay">–</div>
            </div>
        </section>

        <section class="pp-panel" aria-labelledby="lot-title">
            <div class="pp-panel-head">
                <h2 id="lot-title">Parcarea acum</h2>
                <span class="pp-clock" id="clock">Ora parcării --:--</span>
                <span class="pp-live-badge off" id="live-badge"><span class="pp-dot"></span>Conectare…</span>
            </div>
            <canvas id="lot" width="320" height="128" role="img"
                    aria-label="Harta parcării, cu mașinile parcate acum"></canvas>
            <p class="pp-hint">Dă click pe o mașină ca să-i vezi biletul. Ceasul parcării merge de 60 de ori mai repede.</p>
        </section>

        <section class="pp-panel" aria-labelledby="activity-title">
            <h2 id="activity-title">Activitate</h2>
            <ol id="activity" class="pp-activity"></ol>
        </section>
    </div>
    <script type="module" src="${pageContext.request.contextPath}/scripts/live.js"></script>
</t:pageTemplate>
