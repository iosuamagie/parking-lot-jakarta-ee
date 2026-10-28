<%@tag description="The ParkPulse name with a heartbeat line running through it" pageEncoding="UTF-8"%>
<span class="pp-logo">
    <span class="pp-logo-text">ParkPulse</span>
    <%-- pathLength="100" lets the CSS move the bright beat along the line in percent --%>
    <svg class="pp-pulse" viewBox="0 0 200 32" preserveAspectRatio="none" aria-hidden="true" focusable="false">
        <path class="pp-pulse-line" pathLength="100"
              d="M0 18 H72 Q78 12 84 18 H100 L105 22 L111 3 L117 30 L122 18 H134 Q142 11 150 18 H200"/>
        <path class="pp-pulse-beat" pathLength="100"
              d="M0 18 H72 Q78 12 84 18 H100 L105 22 L111 3 L117 30 L122 18 H134 Q142 11 150 18 H200"/>
    </svg>
</span>
