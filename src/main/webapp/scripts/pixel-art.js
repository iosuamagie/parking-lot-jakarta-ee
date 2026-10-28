// Pixel art drawn in code: the cars, the wheelchair symbol and a tiny 3×5 font. No image files.

// Cars are 12×20 pixels, nose to the north (top).
// b = body, d = darker body (roof), l = lighter body, w = glass, k = tire,
// h = headlight, t = taillight, s = taxi sign, x = taxi checker
const MODELS = {
  MINI: [
    '............',
    '............',
    '...bbbbbb...',
    '..hbbbbbbh..',
    '.kbwwwwwwbk.',
    '.kbwwwwwwbk.',
    '..bddddddb..',
    '..bdlllldb..',
    '..bddddddb..',
    '..bddddddb..',
    '..bddddddb..',
    '..bwwwwwwb..',
    '.kbbbbbbbbk.',
    '.kbbbbbbbbk.',
    '..tbbbbbbt..',
    '...bbbbbb...',
    '............',
    '............',
    '............',
    '............'
  ],
  SEDAN: [
    '...bbbbbb...',
    '..hbbbbbbh..',
    '.kbbbbbbbbk.',
    '.kbllllllbk.',
    '.bbwwwwwwbb.',
    '.bwwwwwwwwb.',
    '.bwwwwwwwwb.',
    '.bbddddddbb.',
    '.bbddddddbb.',
    '.bbddddddbb.',
    '.bbddddddbb.',
    '.bbddddddbb.',
    '.bbwwwwwwbb.',
    '.bbwwwwwwbb.',
    '.kbbbbbbbbk.',
    '.kbbbbbbbbk.',
    '.bbbbbbbbbb.',
    '..tbbbbbbt..',
    '...bbbbbb...',
    '............'
  ],
  SUV: [
    '.bbbbbbbbbb.',
    '.hbbbbbbbbh.',
    'kbbbbbbbbbbk',
    'kbbwwwwwwbbk',
    '.bwwwwwwwwb.',
    '.bddddddddb.',
    '.bdlddddldb.',
    '.bdlddddldb.',
    '.bdlddddldb.',
    '.bdlddddldb.',
    '.bdlddddldb.',
    '.bdlddddldb.',
    '.bddddddddb.',
    '.bwwwwwwwwb.',
    'kbbbbbbbbbbk',
    'kbbbbbbbbbbk',
    '.bbbbbbbbbb.',
    '.tbbbbbbbbt.',
    '.bbbbbbbbbb.',
    '............'
  ],
  TAXI: [
    '...bbbbbb...',
    '..hbbbbbbh..',
    '.kbbbbbbbbk.',
    '.kbllllllbk.',
    '.bbwwwwwwbb.',
    '.bwwwwwwwwb.',
    '.bwwwwwwwwb.',
    '.bbddddddbb.',
    '.bbdxsxsdbb.',
    '.bbdsxsxdbb.',
    '.bbddddddbb.',
    '.bbddddddbb.',
    '.bbwwwwwwbb.',
    '.bbwwwwwwbb.',
    '.kbbbbbbbbk.',
    '.kbbbbbbbbk.',
    '.bbbbbbbbbb.',
    '..tbbbbbbt..',
    '...bbbbbb...',
    '............'
  ],
  VAN: [
    '.bbbbbbbbbb.',
    '.hbbbbbbbbh.',
    'kbwwwwwwwwbk',
    'kbwwwwwwwwbk',
    '.bbbbbbbbbb.',
    '.bllllllllb.',
    '.bddddddddb.',
    '.bddddddddb.',
    '.bddddddddb.',
    '.bddddddddb.',
    '.bddddddddb.',
    '.bddddddddb.',
    '.bddddddddb.',
    '.bddddddddb.',
    'kbddddddddbk',
    'kbddddddddbk',
    '.bddddddddb.',
    '.bddddddddb.',
    '.tbbbbbbbbt.',
    '.bbbbbbbbbb.'
  ]
};

export const CAR_W = 12;
export const CAR_H = 20;

function shade(hex, amount) {
  const n = parseInt(hex.slice(1), 16);
  const channel = shift => {
    const v = (n >> shift) & 255;
    const target = amount < 0 ? 0 : 255;
    return Math.round(v + (target - v) * Math.abs(amount));
  };
  return `rgb(${channel(16)},${channel(8)},${channel(0)})`;
}

const sprites = new Map();

/** A 12×20 canvas with the car, cached per model and color. */
export function carSprite(model, color) {
  const key = model + color;
  if (sprites.has(key)) return sprites.get(key);
  const rows = MODELS[model] || MODELS.SEDAN;
  const safeColor = /^#[0-9a-fA-F]{6}$/.test(color) ? color : '#9e9e9e';
  const palette = {
    b: safeColor,
    d: shade(safeColor, -0.22),
    l: shade(safeColor, 0.35),
    w: '#9fd3ff',
    k: '#111318',
    h: '#fff6b0',
    t: '#e11d48',
    s: '#ffffff',
    x: '#111318'
  };
  const canvas = document.createElement('canvas');
  canvas.width = CAR_W;
  canvas.height = CAR_H;
  const ctx = canvas.getContext('2d');
  rows.forEach((row, y) => {
    [...row].forEach((ch, x) => {
      if (palette[ch]) {
        ctx.fillStyle = palette[ch];
        ctx.fillRect(x, y, 1, 1);
      }
    });
  });
  sprites.set(key, canvas);
  return canvas;
}

const WHEELCHAIR = [
  '...##....',
  '...##....',
  '...#.....',
  '...####..',
  '...#.....',
  '.#.#####.',
  '#......#.',
  '#......#.',
  '#......##',
  '.#...#...',
  '..###....'
];
export const WHEELCHAIR_W = 9;

function drawPattern(ctx, rows, x, y, color) {
  ctx.fillStyle = color;
  rows.forEach((row, dy) => {
    [...row].forEach((ch, dx) => {
      if (ch === '#') ctx.fillRect(x + dx, y + dy, 1, 1);
    });
  });
}

export function drawWheelchair(ctx, x, y) {
  drawPattern(ctx, WHEELCHAIR, x, y, '#ffffff');
}

const FONT = {
  A: ['.#.', '#.#', '###', '#.#', '#.#'],
  B: ['##.', '#.#', '##.', '#.#', '##.'],
  0: ['###', '#.#', '#.#', '#.#', '###'],
  1: ['.#.', '##.', '.#.', '.#.', '###'],
  2: ['###', '..#', '###', '#..', '###'],
  3: ['###', '..#', '.##', '..#', '###'],
  4: ['#.#', '#.#', '###', '..#', '..#'],
  5: ['###', '#..', '###', '..#', '###'],
  6: ['###', '#..', '###', '#.#', '###'],
  7: ['###', '..#', '.#.', '.#.', '.#.'],
  8: ['###', '#.#', '###', '#.#', '###'],
  9: ['###', '#.#', '###', '..#', '###']
};

/** Width in pixels of a text in the 3×5 font (1 pixel between letters). */
export function pixelTextWidth(text) {
  return text.length * 4 - 1;
}

export function drawPixelText(ctx, text, x, y, color) {
  [...text].forEach((ch, i) => {
    if (FONT[ch]) drawPattern(ctx, FONT[ch], x + i * 4, y, color);
  });
}
