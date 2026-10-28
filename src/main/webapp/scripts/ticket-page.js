// The /ticket/{code} page: the ticket on the whole screen, refreshed while the car is parked.
import { renderTicket } from './ticket-card.js';

const ctx = document.body.dataset.ctx || '';
const REFRESH_MS = 5000;
const container = document.getElementById('ticket');
const code = container.dataset.ticketCode;
const path = `${ctx}/api/tickets/${encodeURIComponent(code)}`;

async function fetchTicket() {
  const response = await fetch(path, { headers: { Accept: 'application/json' } });
  return response.ok ? response.json() : null;
}

async function start() {
  const [ticket, qr] = await Promise.all([fetchTicket(), fetch(`${path}/qr.svg`).then(r => r.ok ? r.text() : '')]);
  if (!ticket) {
    container.textContent = 'Ticket not found.';
    return;
  }
  const card = renderTicket(container, ticket, qr);
  if (ticket.exitedAt) return;
  const timer = setInterval(async () => {
    try {
      const fresh = await fetchTicket();
      if (!fresh) return;
      card.update(fresh);
      if (fresh.exitedAt) clearInterval(timer);
    } catch {
      // offline for a moment: try again at the next refresh
    }
  }, REFRESH_MS);
}

start();
