// Where everything is on the map, in logical pixels. The canvas is 320×128 and is scaled up without smoothing.
export const W = 320;
export const H = 128;

export const SPOT_W = 22;
export const SPOT_D = 34;
export const FIRST_SPOT_X = 44;
export const SPOTS_PER_ROW = 12;
export const ROW_TOP = { A: 10, B: 84 };

export const LANE_IN = 74;   // eastbound lane, from the entry gate into the lot
export const LANE_OUT = 54;  // westbound lane, toward the exit gate
export const BARRIER_X = 34;
export const BOOTH = { x: 28, y: 60 };
/** The stop lines in front of the barriers, where cars wait (car centers). */
export const GATES = { entry: { x: 22, y: LANE_IN }, exit: { x: 46, y: LANE_OUT } };

const OFF_MAP = -24;
const TURN = 12;
const CURVE_STEPS = 8;

function parse(spot) {
  const match = /^([AB])(\d{2})$/.exec(spot || '');
  if (!match) return null;
  const number = Number(match[2]);
  return number >= 1 && number <= SPOTS_PER_ROW ? { row: match[1], index: number - 1 } : null;
}

export function spotRect(spot) {
  const p = parse(spot);
  if (!p) return null;
  return { x: FIRST_SPOT_X + p.index * SPOT_W, y: ROW_TOP[p.row], w: SPOT_W, h: SPOT_D };
}

export function spotCenter(spot) {
  const r = spotRect(spot);
  return { x: r.x + r.w / 2, y: r.y + r.h / 2 };
}

/** Row A cars park nose north, row B cars nose south. */
export function parkedAngle(spot) {
  return spot.startsWith('A') ? 0 : Math.PI;
}

export function spotAt(x, y) {
  let row = null;
  if (y >= ROW_TOP.A && y < ROW_TOP.A + SPOT_D) row = 'A';
  if (y >= ROW_TOP.B && y < ROW_TOP.B + SPOT_D) row = 'B';
  const index = Math.floor((x - FIRST_SPOT_X) / SPOT_W);
  if (!row || index < 0 || index >= SPOTS_PER_ROW) return null;
  return row + String(index + 1).padStart(2, '0');
}

function curve(from, control, to, reverse) {
  const points = [];
  for (let i = 1; i <= CURVE_STEPS; i++) {
    const t = i / CURVE_STEPS;
    const a = (1 - t) * (1 - t), b = 2 * (1 - t) * t, c = t * t;
    points.push({
      x: a * from.x + b * control.x + c * to.x,
      y: a * from.y + b * control.y + c * to.y,
      reverse
    });
  }
  return points;
}

/**
 * From off the map, through the entry barrier, along the aisle, a turn and straight into the spot.
 * Each point says whether the car drives in reverse to reach it; `gate` marks the stop line.
 */
export function entryPath(spot) {
  const c = spotCenter(spot);
  const turnY = spot.startsWith('A') ? LANE_IN - TURN : LANE_IN + TURN;
  const turnStart = { x: c.x - TURN, y: LANE_IN };
  return [
    { x: OFF_MAP, y: LANE_IN, reverse: false },
    { x: GATES.entry.x, y: LANE_IN, reverse: false, gate: 'entry' },
    { ...turnStart, reverse: false },
    ...curve(turnStart, { x: c.x, y: LANE_IN }, { x: c.x, y: turnY }, false),
    { x: c.x, y: c.y, reverse: false }
  ];
}

/**
 * Backs out of the spot, swings around to face west, drives to the exit barrier and off the map.
 * `joinGate` marks where the car lines up for the exit gate.
 */
export function exitPath(spot) {
  const c = spotCenter(spot);
  const backTo = { x: c.x, y: spot.startsWith('A') ? LANE_OUT - TURN : LANE_OUT + TURN };
  const swingEnd = { x: c.x + TURN, y: LANE_OUT };
  const swing = curve(backTo, { x: c.x, y: LANE_OUT }, swingEnd, true);
  swing[swing.length - 1].joinGate = 'exit';
  return [
    { x: c.x, y: c.y, reverse: false },
    { ...backTo, reverse: true },
    ...swing,
    { x: GATES.exit.x, y: LANE_OUT, reverse: false, gate: 'exit' },
    { x: OFF_MAP, y: LANE_OUT, reverse: false }
  ];
}
