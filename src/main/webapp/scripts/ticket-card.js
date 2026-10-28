// Draws a parking ticket (used by the popup on the live page and by the /ticket/{code} page).
import { amountBani, formatRon } from './tariff.js';
import { animationsOn } from './motion.js';

const ctx = document.body.dataset.ctx || '';
const dateTime = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Europe/Bucharest', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
});

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function row(label) {
  const line = el('div', 'pp-ticket-row');
  const value = el('strong');
  line.append(el('span', null, label), value);
  return { line, value };
}

function formatDuration(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds));
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60;
  return `${h ? h + 'h ' : ''}${String(m).padStart(2, '0')}m ${String(sec).padStart(2, '0')}s`;
}

/** Turns the SVG text from the server into an element, without putting it through innerHTML. */
function qrElement(qrSvgText) {
  const parsed = new DOMParser().parseFromString(qrSvgText, 'image/svg+xml').documentElement;
  if (parsed.nodeName !== 'svg') return el('div', 'pp-qr-missing', 'QR code unavailable');
  const svg = document.importNode(parsed, true);
  svg.setAttribute('role', 'img');
  svg.setAttribute('aria-label', 'The ticket QR code');
  return svg;
}

/**
 * @returns {{update(ticket): void, destroy(): void, element: HTMLElement}}
 */
export function renderTicket(container, ticket, qrSvgText) {
  let current = ticket;
  let fetchedAt = performance.now();

  const card = el('article', 'pp-ticket');
  const top = el('div', 'pp-ticket-top');
  const head = el('div', 'pp-ticket-head');
  const title = el('span', null, 'PARKING TICKET · ');
  const number = el('b');
  title.append(number);
  head.append(title);

  const body = el('div', 'pp-ticket-body');
  const firstLine = el('div', 'pp-ticket-first');
  const plate = el('div', 'pp-plate');
  const plateText = el('span');
  plate.append(el('b', null, 'RO'), plateText);
  const chips = el('div', 'pp-chips');
  firstLine.append(plate, chips);

  const spot = row('Spot');
  const entered = row('Entered');
  const exited = row('Left');
  const duration = row('Stay');
  duration.value.classList.add('pp-tabular');
  const dueLabel = el('div', 'pp-due-label');
  const due = el('div', 'pp-due');
  const note = el('div', 'pp-ticket-note', 'First 15 minutes free · then 6 RON/hour');
  body.append(firstLine, spot.line, entered.line, exited.line, duration.line, dueLabel, due, note);
  top.append(head, body);

  const bottom = el('div', 'pp-ticket-bottom');
  const qr = el('div', animationsOn() ? 'pp-qr printing' : 'pp-qr');
  const svg = qrElement(qrSvgText);
  qr.style.setProperty('--rows', svg.querySelectorAll ? svg.querySelectorAll('.qr-row').length : 0);
  qr.append(svg, el('div', 'pp-qr-scan'));
  const qrNote = el('div', 'pp-qr-note', 'Scan to follow the ticket on your phone');
  const openPage = el('a', 'pp-open-page', 'Open in a new page ↗');
  openPage.target = '_blank';
  openPage.rel = 'noopener';
  const receipt = el('a', 'pp-receipt');
  bottom.append(qr, qrNote, openPage, receipt);

  card.append(top, el('div', 'pp-perf'), bottom);
  container.replaceChildren(card);

  function fill() {
    const t = current;
    const isParked = !t.exitedAt;
    number.textContent = t.ticketNumber;
    plateText.textContent = t.plate;
    spot.value.textContent = t.spot;
    entered.value.textContent = dateTime.format(new Date(t.enteredAt));
    exited.line.hidden = isParked;
    if (!isParked) exited.value.textContent = dateTime.format(new Date(t.exitedAt));

    chips.replaceChildren(el('span', isParked ? 'pp-chip pp-chip-parked' : 'pp-chip pp-chip-paid',
      isParked ? '● Parked' : 'Paid · left'));
    if (t.accessiblePermit) chips.append(el('span', 'pp-chip pp-chip-permit', '♿ Permit'));

    dueLabel.textContent = isParked ? 'Due so far' : 'Paid';
    openPage.href = `${ctx}/ticket/${encodeURIComponent(t.ticketCode)}`;
    if (isParked) {
      receipt.removeAttribute('href');
      receipt.setAttribute('aria-disabled', 'true');
      receipt.textContent = 'Receipt after the car leaves';
    } else {
      receipt.href = `${ctx}/api/tickets/${encodeURIComponent(t.ticketCode)}/receipt.pdf`;
      receipt.removeAttribute('aria-disabled');
      receipt.textContent = '⬇ Download the receipt (PDF)';
    }
    tick();
  }

  function tick() {
    const t = current;
    if (t.exitedAt) {
      duration.value.textContent = formatDuration((Date.parse(t.exitedAt) - Date.parse(t.enteredAt)) / 1000);
      due.textContent = t.amountBani > 0 ? formatRon(t.amountBani) : 'free';
      return;
    }
    // The parking clock keeps running between two fetches, clockSpeed times faster than real time
    const seconds = (Date.parse(t.clockNow) - Date.parse(t.enteredAt)) / 1000
      + (performance.now() - fetchedAt) / 1000 * t.clockSpeed;
    duration.value.textContent = formatDuration(seconds);
    due.textContent = formatRon(amountBani(seconds));
  }

  fill();
  const timer = setInterval(tick, 250);

  return {
    element: card,
    update(ticketNow) {
      current = ticketNow;
      fetchedAt = performance.now();
      fill();
    },
    destroy() {
      clearInterval(timer);
    }
  };
}
