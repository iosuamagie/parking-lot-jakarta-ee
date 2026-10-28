// Starts the live page: listens to the server-sent events and keeps the map, the cards and the activity list current.
import { createLot } from './lot-renderer.js';
import { spotAt, spotRect, W, H } from './lot-geometry.js';
import { openTicket, ticketExited, showToast } from './ticket-popup.js';
import { formatLei } from './tariff.js';
import { animationsOn, setAnimations } from './motion.js';

const ACTIVITY_LIMIT = 20;
const ctx = document.body.dataset.ctx || '';
const canvas = document.getElementById('lot');
let reducedMotion = !animationsOn();
const lot = createLot(canvas, { reducedMotion });

const timeFormat = new Intl.DateTimeFormat('ro-RO', { timeZone: 'Europe/Bucharest', hour: '2-digit', minute: '2-digit' });
const parkedBySpot = new Map();
let clock = { at: 0, speed: 60, realAt: performance.now() };

const $ = id => document.getElementById(id);

function parkingNow() {
  return new Date(clock.at + (performance.now() - clock.realAt) * clock.speed);
}

function syncClock(isoNow, speed) {
  clock = { at: Date.parse(isoNow), speed, realAt: performance.now() };
}

function formatStay(minutes) {
  if (minutes < 60) return `${minutes} min`;
  return `${Math.floor(minutes / 60)}h ${String(minutes % 60).padStart(2, '0')}m`;
}

function setText(id, text) {
  const el = $(id);
  if (el.textContent === text) return;
  el.textContent = text;
  if (!reducedMotion) {
    el.classList.remove('pp-bump');
    void el.offsetWidth; // restarts the animation
    el.classList.add('pp-bump');
  }
}

function renderStats(stats) {
  setText('kpi-free', String(stats.free));
  $('kpi-capacity').textContent = stats.capacity;
  $('kpi-free-bar').style.width = `${Math.round(100 * stats.free / stats.capacity)}%`;
  setText('kpi-revenue', formatLei(stats.revenueTodayBani));
  setText('kpi-cars', String(stats.carsToday));
  setText('kpi-stay', stats.averageStayMinutes > 0 ? formatStay(stats.averageStayMinutes) : '–');

  const trend = $('kpi-trend');
  const yesterday = stats.revenueYesterdaySameTimeBani;
  if (yesterday > 0) {
    const change = Math.round(100 * (stats.revenueTodayBani - yesterday) / yesterday);
    trend.textContent = `${change >= 0 ? '▲' : '▼'} ${Math.abs(change)}% față de ieri la aceeași oră`;
    trend.className = 'pp-kpi-trend ' + (change >= 0 ? 'up' : 'down');
  } else {
    trend.textContent = '';
  }
}

function activityItem(activity, isNew) {
  const li = document.createElement('li');
  const entered = activity.type === 'entered';
  li.className = 'pp-act' + (isNew && !reducedMotion ? ' new' : '');
  li.dataset.code = activity.ticketCode;
  li.dataset.spot = activity.spot;
  li.tabIndex = 0;

  const kind = document.createElement('span');
  kind.className = entered ? 'pp-act-in' : 'pp-act-out';
  kind.textContent = entered ? '↘ Intrare' : '↗ Ieșire';
  const plate = document.createElement('span');
  plate.className = 'pp-plate-text';
  plate.textContent = activity.plate;
  const detail = document.createElement('span');
  if (entered) {
    detail.textContent = activity.spot;
  } else {
    detail.textContent = activity.amountBani > 0 ? formatLei(activity.amountBani) : 'gratuit';
  }
  const time = document.createElement('span');
  time.className = 'pp-act-time';
  time.textContent = timeFormat.format(new Date(activity.at));

  li.append(kind, ' ', plate, ' · ', detail, document.createElement('br'), time);
  return li;
}

function renderActivity(list) {
  $('activity').replaceChildren(...list.slice(0, ACTIVITY_LIMIT).map(a => activityItem(a, false)));
}

function addActivity(activity) {
  const list = $('activity');
  list.prepend(activityItem(activity, true));
  while (list.children.length > ACTIVITY_LIMIT) list.lastElementChild.remove();
}

function applySnapshot(snapshot) {
  syncClock(snapshot.clockNow, snapshot.clockSpeed);
  parkedBySpot.clear();
  snapshot.cars.forEach(car => parkedBySpot.set(car.spot, car));
  lot.setSnapshot(snapshot);
  renderStats(snapshot.stats);
  renderActivity(snapshot.activity);
}

function onEntered(car) {
  parkedBySpot.set(car.spot, car);
  syncClock(car.enteredAt, clock.speed);
  // While the tab is hidden nothing is animated; the map is redrawn from a snapshot when it comes back
  if (!document.hidden) lot.carEntered(car);
  addActivity({ type: 'entered', plate: car.plate, spot: car.spot, ticketCode: car.ticketCode, at: car.enteredAt });
}

function onExited(activity) {
  parkedBySpot.delete(activity.spot);
  syncClock(activity.at, clock.speed);
  if (!document.hidden) lot.carExited(activity);
  addActivity(activity);
  ticketExited(activity);
}

function setLive(isLive) {
  const badge = $('live-badge');
  badge.classList.toggle('off', !isLive);
  badge.lastChild.textContent = isLive ? 'LIVE' : 'Reconectare…';
}

const RETRY_MIN_MS = 5000;
const RETRY_MAX_MS = 30000;
let retryMs = RETRY_MIN_MS;

function connect() {
  const events = new EventSource(`${ctx}/api/events`);
  events.addEventListener('snapshot', e => {
    applySnapshot(JSON.parse(e.data));
    setLive(true);
    retryMs = RETRY_MIN_MS;
  });
  events.addEventListener('entered', e => onEntered(JSON.parse(e.data)));
  events.addEventListener('exited', e => onExited(JSON.parse(e.data)));
  events.addEventListener('stats', e => renderStats(JSON.parse(e.data)));
  events.onerror = () => {
    setLive(false);
    // After a dropped connection the browser reconnects by itself (and the server starts again with a
    // snapshot). After an error answer, like 503 when too many pages are open, it gives up: try again later.
    if (events.readyState === EventSource.CLOSED) {
      events.close();
      setTimeout(connect, retryMs);
      retryMs = Math.min(retryMs * 2, RETRY_MAX_MS);
    }
  };
}

async function refresh() {
  try {
    const response = await fetch(`${ctx}/api/lot`, { headers: { Accept: 'application/json' } });
    if (response.ok) applySnapshot(await response.json());
  } catch {
    // the live feed will send a fresh snapshot when it reconnects
  }
}

document.addEventListener('visibilitychange', () => {
  if (!document.hidden) refresh();
});

function tickClock() {
  $('clock').textContent = `Ora parcării ${timeFormat.format(parkingNow())} · ×${clock.speed}`;
}
setInterval(tickClock, 1000);

function spotFromEvent(event) {
  const rect = canvas.getBoundingClientRect();
  return spotAt((event.clientX - rect.left) * W / rect.width, (event.clientY - rect.top) * H / rect.height);
}

canvas.addEventListener('mousemove', e => {
  canvas.style.cursor = parkedBySpot.has(spotFromEvent(e)) ? 'pointer' : 'default';
});

/** Where a spot is on the screen, so the ticket can grow out of it. */
function spotOnScreen(spot) {
  const r = spotRect(spot);
  const box = canvas.getBoundingClientRect();
  const k = box.width / W;
  return { left: box.left + r.x * k, top: box.top + r.y * k, width: r.w * k, height: r.h * k };
}

function choose(spot, code, origin) {
  openTicket(code, origin);
  lot.select(spot);
}

canvas.addEventListener('click', e => {
  const spot = spotFromEvent(e);
  if (!spot) return;
  const car = parkedBySpot.get(spot);
  if (car) {
    choose(spot, car.ticketCode, spotOnScreen(spot));
  } else {
    lot.select(null);
    showToast(`Locul ${spot} e liber`);
  }
});

$('activity').addEventListener('click', e => {
  const item = e.target.closest('.pp-act');
  if (item) choose(item.dataset.spot, item.dataset.code, item.getBoundingClientRect());
});
$('activity').addEventListener('keydown', e => {
  const item = e.target.closest('.pp-act');
  if (item && (e.key === 'Enter' || e.key === ' ')) {
    e.preventDefault();
    choose(item.dataset.spot, item.dataset.code, item.getBoundingClientRect());
  }
});

document.addEventListener('ticket:closed', () => lot.select(null));

const motionToggle = $('motion-toggle');
function showMotionChoice() {
  motionToggle.setAttribute('aria-pressed', String(!reducedMotion));
  motionToggle.textContent = reducedMotion ? 'Animații oprite' : 'Animații pornite';
}
motionToggle.addEventListener('click', () => setAnimations(reducedMotion));
document.addEventListener('motion:change', e => {
  reducedMotion = !e.detail.on;
  lot.setReducedMotion(reducedMotion);
  showMotionChoice();
});
showMotionChoice();

connect();
tickClock();
