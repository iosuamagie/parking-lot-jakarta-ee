// The ticket popup of the live page: it grows out of the clicked car (FLIP), the QR code "prints" row by row.
import { renderTicket } from './ticket-card.js';
import { animationsOn } from './motion.js';

const ctx = document.body.dataset.ctx || '';
const OPEN_MS = 520;
const CLOSE_MS = 340;

let overlay = null;
let holder = null;
let card = null;
let currentCode = null;
let fromRect = null;
let opener = null;
let toastTimer = null;

function build() {
  overlay = document.createElement('div');
  overlay.className = 'pp-ticket-overlay';
  overlay.hidden = true;
  const backdrop = document.createElement('div');
  backdrop.className = 'pp-ticket-backdrop';
  backdrop.addEventListener('click', closeTicket);
  holder = document.createElement('div');
  holder.className = 'pp-ticket-holder';
  holder.setAttribute('role', 'dialog');
  holder.setAttribute('aria-modal', 'true');
  holder.setAttribute('aria-label', 'Parking ticket');
  overlay.append(backdrop, holder);
  document.body.append(overlay);
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && currentCode) closeTicket();
  });
}

async function load(code) {
  const path = `${ctx}/api/tickets/${encodeURIComponent(code)}`;
  try {
    const [ticket, qr] = await Promise.all([fetch(path), fetch(`${path}/qr.svg`)]);
    if (!ticket.ok || !qr.ok) return null;
    return { ticket: await ticket.json(), qr: await qr.text() };
  } catch {
    return null;
  }
}

function flip(rect, target) {
  return `translate(${rect.left - target.left}px, ${rect.top - target.top}px) `
    + `scale(${rect.width / target.width}, ${rect.height / target.height})`;
}

export function showToast(message) {
  let toast = document.querySelector('.pp-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.className = 'pp-toast';
    toast.setAttribute('role', 'status');
    document.body.append(toast);
  }
  toast.textContent = message;
  toast.classList.add('on');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('on'), 1800);
}

/**
 * @param rect where the ticket grows from (a DOMRect-like {left, top, width, height})
 */
export async function openTicket(code, rect) {
  if (!overlay) build();
  if (currentCode) closeNow();
  opener = document.activeElement;
  currentCode = code;
  fromRect = rect;

  const data = await load(code);
  if (currentCode !== code) return; // another ticket was opened meanwhile
  if (!data) {
    currentCode = null;
    showToast('Ticket not found');
    return;
  }

  card = renderTicket(holder, data.ticket, data.qr);
  const close = document.createElement('button');
  close.type = 'button';
  close.className = 'pp-ticket-close';
  close.setAttribute('aria-label', 'Close');
  close.textContent = '✕';
  close.addEventListener('click', closeTicket);
  card.element.querySelector('.pp-ticket-head').append(close);

  overlay.hidden = false;
  requestAnimationFrame(() => overlay.classList.add('on'));
  if (animationsOn() && fromRect) {
    const target = card.element.getBoundingClientRect();
    card.element.animate(
      [{ transform: flip(fromRect, target), opacity: 0.3 }, { transform: 'none', opacity: 1 }],
      { duration: OPEN_MS, easing: 'cubic-bezier(.2,.9,.25,1)' });
  }
  close.focus({ preventScroll: true });
}

function closeNow() {
  if (card) card.destroy();
  card = null;
  currentCode = null;
  overlay.classList.remove('on');
  overlay.hidden = true;
  holder.replaceChildren();
  document.dispatchEvent(new CustomEvent('ticket:closed'));
  if (opener && opener.focus) opener.focus({ preventScroll: true });
}

export function closeTicket() {
  if (!currentCode || !card) {
    currentCode = null;
    return;
  }
  overlay.classList.remove('on');
  if (!animationsOn() || !fromRect) {
    closeNow();
    return;
  }
  const target = card.element.getBoundingClientRect();
  const animation = card.element.animate(
    [{ transform: 'none', opacity: 1 }, { transform: flip(fromRect, target), opacity: 0 }],
    { duration: CLOSE_MS, easing: 'cubic-bezier(.5,0,.75,0)', fill: 'forwards' });
  const code = currentCode;
  animation.onfinish = () => {
    if (currentCode === code) closeNow();
  };
}

/** The car of the open ticket has just left: show the final amount. */
export async function ticketExited(activity) {
  if (!card || activity.ticketCode !== currentCode) return;
  const data = await load(activity.ticketCode);
  if (data && card && currentCode === activity.ticketCode) card.update(data.ticket);
}
