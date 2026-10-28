// Draws the lot on a canvas and animates cars driving in and out, with the barriers and a queue at each gate.
import {
  W, H, SPOT_W, SPOT_D, FIRST_SPOT_X, SPOTS_PER_ROW, ROW_TOP, LANE_IN, LANE_OUT, BARRIER_X, BOOTH,
  spotRect, spotCenter, parkedAngle, entryPath, exitPath
} from './lot-geometry.js';
import { carSprite, drawWheelchair, drawPixelText, pixelTextWidth, WHEELCHAIR_W, CAR_W, CAR_H } from './pixel-art.js';

const SPEED = 60;           // px per second
const SLOW_DOWN = 10;       // px before a stop where the car brakes
const QUEUE_GAP = 24;       // px between cars waiting at a gate
const CLEAR = 22;           // px past the stop line where a car has cleared the barrier
const BARRIER_MS = 400;
const RAISE_DISTANCE = 40;  // the barrier rises when the first car in line is this close
const ANGLE_STEP = Math.PI / 8; // 22.5°

const COLORS = {
  asphalt: '#3b4150',
  grass: '#2f6b45',
  grassDark: '#285d3c',
  line: '#e8e8e8',
  dash: '#f1c40f',
  accessible: '#2456c9',
  label: '#c9ced8',
  booth: '#cfd6e4',
  boothRoof: '#4f46e5'
};

function drawBackground(accessible) {
  const canvas = document.createElement('canvas');
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d');

  ctx.fillStyle = COLORS.grass;
  ctx.fillRect(0, 0, W, H);
  for (let i = 0; i < 70; i++) {
    // a few darker tufts, always in the same places
    const x = (i * 53) % W, y = (i * 29) % H;
    ctx.fillStyle = COLORS.grassDark;
    ctx.fillRect(x, y, 1, 1);
  }

  const lotRight = FIRST_SPOT_X + SPOTS_PER_ROW * SPOT_W;
  ctx.fillStyle = COLORS.asphalt;
  ctx.fillRect(40, ROW_TOP.A, lotRight - 40, ROW_TOP.B + SPOT_D - ROW_TOP.A);
  ctx.fillRect(0, LANE_OUT - 10, 40, LANE_IN - LANE_OUT + 20);

  // the dashed middle line of the aisle and of the gate road
  ctx.fillStyle = COLORS.dash;
  for (let x = 2; x < lotRight - 6; x += 12) {
    if (x > 20 && x < 40) continue;
    ctx.fillRect(x, (LANE_OUT + LANE_IN) / 2, 6, 1);
  }

  for (const row of ['A', 'B']) {
    for (let i = 0; i < SPOTS_PER_ROW; i++) {
      const spot = row + String(i + 1).padStart(2, '0');
      const r = spotRect(spot);
      if (accessible.has(spot)) {
        ctx.fillStyle = COLORS.accessible;
        ctx.fillRect(r.x + 1, r.y + 1, r.w - 1, r.h - 1);
        drawWheelchair(ctx, r.x + Math.floor((r.w - WHEELCHAIR_W) / 2) + 1, r.y + 12);
      }
      ctx.fillStyle = COLORS.line;
      ctx.fillRect(r.x, r.y, 1, r.h);
      // the number is painted at the aisle end, where a parked car does not cover it
      const labelY = row === 'A' ? r.y + r.h - 7 : r.y + 2;
      const labelX = r.x + Math.round((r.w - pixelTextWidth(spot)) / 2) + 1;
      drawPixelText(ctx, spot, labelX, labelY, accessible.has(spot) ? '#ffffff' : COLORS.label);
    }
    ctx.fillStyle = COLORS.line;
    const r = spotRect(row + String(SPOTS_PER_ROW).padStart(2, '0'));
    ctx.fillRect(r.x + r.w, r.y, 1, r.h);
    // the curb at the back of the row
    ctx.fillRect(FIRST_SPOT_X, row === 'A' ? ROW_TOP.A : ROW_TOP.B + SPOT_D - 1, lotRight - FIRST_SPOT_X + 1, 1);
  }

  // the booth between the two barriers
  ctx.fillStyle = COLORS.booth;
  ctx.fillRect(BOOTH.x - 6, BOOTH.y, 8, 8);
  ctx.fillStyle = COLORS.boothRoof;
  ctx.fillRect(BOOTH.x - 7, BOOTH.y - 1, 10, 2);
  ctx.fillStyle = '#9fd3ff';
  ctx.fillRect(BOOTH.x - 4, BOOTH.y + 3, 4, 2);
  return canvas;
}

/** Turns a list of points into a route a car can follow by distance. */
function makeRoute(points) {
  const dist = [0];
  for (let i = 1; i < points.length; i++) {
    dist.push(dist[i - 1] + Math.hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y));
  }
  const gateIndex = points.findIndex(p => p.gate);
  const joinIndex = points.findIndex(p => p.joinGate);
  return {
    points,
    dist,
    total: dist[dist.length - 1],
    gate: points[gateIndex].gate,
    gateDist: dist[gateIndex],
    joinDist: joinIndex >= 0 ? dist[joinIndex] : 0
  };
}

function positionOn(route, d) {
  const { points, dist } = route;
  let i = 1;
  while (i < points.length - 1 && dist[i] < d) i++;
  const a = points[i - 1], b = points[i];
  const span = dist[i] - dist[i - 1] || 1;
  const t = Math.min(1, Math.max(0, (d - dist[i - 1]) / span));
  let angle = Math.atan2(b.x - a.x, -(b.y - a.y));
  if (b.reverse) angle += Math.PI;
  return {
    x: a.x + (b.x - a.x) * t,
    y: a.y + (b.y - a.y) * t,
    angle: Math.round(angle / ANGLE_STEP) * ANGLE_STEP
  };
}

export function createLot(canvas, { reducedMotion: startReduced = false } = {}) {
  let reducedMotion = startReduced;
  const ctx = canvas.getContext('2d');
  let accessible = new Set(['A01', 'A02', 'A03']);
  let background = drawBackground(accessible);
  let parked = new Map();   // spot → car
  let movers = [];
  const gates = { entry: { queue: [], level: 0 }, exit: { queue: [], level: 0 } };
  let selected = null;
  let scale = 1;
  let last = performance.now();

  function resize() {
    const ratio = window.devicePixelRatio || 1;
    scale = Math.max(1, Math.ceil(canvas.clientWidth * ratio / W));
    canvas.width = W * scale;
    canvas.height = H * scale;
  }
  new ResizeObserver(resize).observe(canvas);
  resize();

  function toCar(data) {
    return { ...data, sprite: carSprite(data.model, data.color) };
  }

  function leaveQueue(mover) {
    const queue = gates[mover.route.gate].queue;
    const i = queue.indexOf(mover);
    if (i >= 0) queue.splice(i, 1);
  }

  function finishEntryOn(spot) {
    const mover = movers.find(m => m.kind === 'enter' && m.car.spot === spot);
    if (!mover) return;
    leaveQueue(mover);
    movers = movers.filter(m => m !== mover);
    parked.set(spot, mover.car);
  }

  function update(dt) {
    for (const mover of movers) {
      const gate = gates[mover.route.gate];
      let limit = mover.route.total;
      if (!mover.passed && mover.queued) {
        const place = gate.queue.indexOf(mover);
        if (place > 0 || gate.level < 1) limit = mover.route.gateDist - QUEUE_GAP * Math.max(0, place);
      } else if (!mover.passed && !mover.queued) {
        limit = Math.min(limit, mover.route.gateDist - QUEUE_GAP * gate.queue.length);
      }
      const remaining = limit - mover.d;
      if (remaining > 0) {
        const brake = remaining < SLOW_DOWN ? Math.max(0.3, remaining / SLOW_DOWN) : 1;
        mover.d = Math.min(limit, mover.d + SPEED * brake * dt);
      }
      if (!mover.queued && mover.d >= mover.route.joinDist) {
        gate.queue.push(mover);
        mover.queued = true;
      }
      if (mover.queued && !mover.passed && mover.d >= mover.route.gateDist + CLEAR) {
        mover.passed = true;
        leaveQueue(mover);
      }
    }

    const done = movers.filter(m => m.d >= m.route.total);
    for (const mover of done) {
      if (mover.kind === 'enter') parked.set(mover.car.spot, mover.car);
    }
    movers = movers.filter(m => m.d < m.route.total);

    for (const gate of Object.values(gates)) {
      const first = gate.queue[0];
      const wanted = first && first.d >= first.route.gateDist - RAISE_DISTANCE ? 1 : 0;
      const step = dt * 1000 / BARRIER_MS;
      gate.level = wanted > gate.level ? Math.min(1, gate.level + step) : Math.max(0, gate.level - step);
    }
  }

  function drawCar(car, x, y, angle) {
    ctx.save();
    ctx.translate(Math.round(x), Math.round(y));
    ctx.rotate(angle);
    ctx.fillStyle = 'rgba(0,0,0,.28)';
    ctx.fillRect(-CAR_W / 2 + 2, -CAR_H / 2 + 1, CAR_W - 3, CAR_H - 1);
    ctx.drawImage(car.sprite, -CAR_W / 2, -CAR_H / 2);
    ctx.restore();
  }

  function drawBarrier(pivotY, direction, level) {
    // Seen from above, a rising arm looks shorter and shorter
    const length = Math.round(2 + 16 * (1 - level));
    ctx.fillStyle = '#5b6170';
    ctx.fillRect(BARRIER_X - 1, pivotY - 1, 3, 3);
    for (let i = 0; i < length; i++) {
      ctx.fillStyle = Math.floor(i / 3) % 2 === 0 ? '#e74c3c' : '#ffffff';
      ctx.fillRect(BARRIER_X, pivotY + direction * (i + 2), 1, 1);
    }
  }

  function draw(now) {
    ctx.setTransform(scale, 0, 0, scale, 0, 0);
    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(background, 0, 0);

    if (selected) {
      const r = spotRect(selected);
      if (r) {
        const pulse = reducedMotion ? 1 : 0.55 + 0.45 * Math.sin(now / 160);
        ctx.strokeStyle = `rgba(165,180,252,${pulse})`;
        ctx.lineWidth = 1;
        ctx.strokeRect(r.x + 1.5, r.y + 1.5, r.w - 2, r.h - 2);
      }
    }

    for (const car of parked.values()) {
      const c = spotCenter(car.spot);
      drawCar(car, c.x, c.y, parkedAngle(car.spot));
    }
    for (const mover of movers) {
      const p = positionOn(mover.route, mover.d);
      drawCar(mover.car, p.x, p.y, p.angle);
    }

    drawBarrier(LANE_IN - 9, 1, gates.entry.level);
    drawBarrier(LANE_OUT + 9, -1, gates.exit.level);
  }

  function frame(now) {
    const dt = Math.min(0.05, (now - last) / 1000);
    last = now;
    update(dt);
    draw(now);
    requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);

  return {
    /** Draws the lot exactly as the server says, dropping any animation in progress. */
    setSnapshot(snapshot) {
      const newAccessible = new Set(snapshot.accessibleSpots || []);
      if ([...newAccessible].join() !== [...accessible].join()) {
        accessible = newAccessible;
        background = drawBackground(accessible);
      }
      movers = [];
      gates.entry.queue = [];
      gates.exit.queue = [];
      parked = new Map(snapshot.cars.map(car => [car.spot, toCar(car)]));
    },

    carEntered(car) {
      // A car still driving into this spot is a stale event: put it down. A car still leaving the spot is
      // another car and keeps driving out (dropping it here would leave it stuck in the exit queue).
      finishEntryOn(car.spot);
      if (reducedMotion) {
        parked.set(car.spot, toCar(car));
        return;
      }
      parked.delete(car.spot);
      const mover = { kind: 'enter', car: toCar(car), route: makeRoute(entryPath(car.spot)), d: 0, queued: false, passed: false };
      movers.push(mover);
    },

    carExited(activity) {
      // A car that is still driving in is put on its spot first, then it leaves
      finishEntryOn(activity.spot);
      const car = parked.get(activity.spot);
      if (!car) return;
      parked.delete(activity.spot);
      if (reducedMotion) return;
      movers.push({ kind: 'exit', car, route: makeRoute(exitPath(activity.spot)), d: 0, queued: false, passed: false });
    },

    select(spot) {
      selected = spot;
    },

    /** Cars still on their way finish where they are going when motion is switched off. */
    setReducedMotion(value) {
      reducedMotion = value;
    }
  };
}
