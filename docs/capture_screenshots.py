"""Takes the README screenshots and the live GIF of ParkPulse with Playwright.

Needs Python with playwright 1.64.0 and pillow 12.3.0 (then: py -m playwright install chromium).
Run it against a fresh copy of the app, so the billing page shows clean demo data:

    PARKPULSE_PORT=8082 docker compose -p parkpulse-shots up -d --build
    py docs/capture_screenshots.py http://localhost:8082 docs/images
    docker compose -p parkpulse-shots down -v
"""
import io
import os
import sys
import time
from pathlib import Path

from PIL import Image
from playwright.sync_api import sync_playwright

BASE = sys.argv[1].rstrip('/')
OUT = Path(sys.argv[2])
OUT.mkdir(parents=True, exist_ok=True)
VIEWPORT = {'width': 1366, 'height': 820}


def wait_for_live(page):
    page.goto(f'{BASE}/')
    page.wait_for_selector('#live-badge:not(.off)', timeout=30000)
    page.wait_for_selector('#activity li', timeout=30000)
    page.wait_for_timeout(2500)


def live_gif(page, seconds=22, fps=10):
    """Records the map card only, at its real size, so the pixel cars stay crisp."""
    box = page.locator('#lot').locator('xpath=ancestor::section[1]').bounding_box()
    clip = {k: round(box[k]) for k in ('x', 'y', 'width', 'height')}
    frames = []
    step = 1 / fps
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        started = time.monotonic()
        frames.append(Image.open(io.BytesIO(page.screenshot(clip=clip))).convert('RGB'))
        time.sleep(max(0, step - (time.monotonic() - started)))
    # One palette for the whole GIF, taken from frames spread over the recording
    samples = frames[::max(1, len(frames) // 8)]
    mosaic = Image.new('RGB', (frames[0].width, frames[0].height * len(samples)))
    for i, f in enumerate(samples):
        mosaic.paste(f, (0, i * f.height))
    palette = mosaic.quantize(colors=256, method=Image.MEDIANCUT)
    gif = [f.quantize(palette=palette, dither=Image.Dither.NONE) for f in frames]
    gif[0].save(OUT / 'live.gif', save_all=True, append_images=gif[1:],
                duration=round(1000 / fps), loop=0, optimize=True)
    print('live.gif', len(frames), 'frames', frames[0].size)


def click_longest_parked(page):
    """Clicks the car that has been parked the longest, so its ticket already has something to pay."""
    point = page.evaluate("""async () => {
        const { spotRect, W } = await import(document.body.dataset.ctx + '/scripts/lot-geometry.js');
        const lot = await (await fetch(document.body.dataset.ctx + '/api/lot')).json();
        const car = lot.cars.reduce((a, b) => a.enteredAt < b.enteredAt ? a : b);
        const r = spotRect(car.spot);
        const box = document.getElementById('lot').getBoundingClientRect();
        const k = box.width / W;
        return { x: box.left + (r.x + r.w / 2) * k, y: box.top + (r.y + r.h / 2) * k };
    }""")
    page.mouse.click(point['x'], point['y'])


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(args=['--lang=ro-RO'])
        context = browser.new_context(viewport=VIEWPORT, device_scale_factor=1, locale='ro-RO',
                                      timezone_id='Europe/Bucharest')
        page = context.new_page()

        wait_for_live(page)
        page.screenshot(path=OUT / 'live.png', clip={'x': 0, 'y': 0, 'width': 1366, 'height': 720})
        live_gif(page)

        click_longest_parked(page)
        page.wait_for_selector('.pp-ticket-overlay.on', timeout=10000)
        page.wait_for_timeout(2500)
        page.screenshot(path=OUT / 'ticket.png', clip={'x': 0, 'y': 0, 'width': 1366, 'height': 760})

        page.goto(f'{BASE}/api-docs')
        page.wait_for_selector('#swagger .opblock', timeout=30000)
        page.wait_for_timeout(1000)
        page.screenshot(path=OUT / 'api-docs.png')

        page.goto(f'{BASE}/Account')
        page.fill('#username', 'billing')
        page.fill('#password', os.environ.get('PARKPULSE_DEMO_PASSWORD', 'parkpulse-demo'))
        page.click('button[type=submit]')
        page.wait_for_url('**/billing**', timeout=15000)
        page.wait_for_timeout(1500)
        page.screenshot(path=OUT / 'billing.png')

        browser.close()


main()
