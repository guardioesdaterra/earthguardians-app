import { createRequire } from 'node:module'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { mkdir, readFile, writeFile } from 'node:fs/promises'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const repoRoot = resolve(scriptDir, '../..')
const require = createRequire(new URL('../../web/package.json', import.meta.url))
const { chromium } = require('playwright')

const WIDTH_PX = 1080
const HEIGHT_PX = 2187
const DEVICE_SCALE_FACTOR = 2.625
const CSS_CLIP = {
  x: 0,
  y: 0,
  width: WIDTH_PX / DEVICE_SCALE_FACTOR,
  height: HEIGHT_PX / DEVICE_SCALE_FACTOR,
}
const url = process.env.PWA_HOME_URL || 'http://127.0.0.1:4173/'
const screenshotPath = resolve(repoRoot, process.env.PWA_HOME_SCREENSHOT || 'artifacts/pwa-home.png')
const masksPath = resolve(repoRoot, process.env.PWA_HOME_MASKS || 'artifacts/pwa-home-masks.json')
const feedPath = resolve(repoRoot, 'tests/fixtures/pwa-home/feed.json')
const feed = await readFile(feedPath, 'utf8')

await mkdir(dirname(screenshotPath), { recursive: true })
await mkdir(dirname(masksPath), { recursive: true })

const browser = await chromium.launch({ headless: true })
try {
  const context = await browser.newContext({
    viewport: { width: 412, height: 834 },
    deviceScaleFactor: DEVICE_SCALE_FACTOR,
    isMobile: true,
    hasTouch: true,
    colorScheme: 'light',
    locale: 'en-US',
    timezoneId: 'UTC',
    reducedMotion: 'reduce',
    serviceWorkers: 'block',
  })
  const page = await context.newPage()
  await page.route('**/eg-cache/eg_feed_cache.json', route => route.fulfill({
    status: 200,
    contentType: 'application/json; charset=utf-8',
    body: feed,
  }))

  await page.goto(url, { waitUntil: 'networkidle', timeout: 60_000 })
  await page.locator('.home-hero').waitFor({ state: 'visible', timeout: 30_000 })
  await page.getByRole('heading', { name: 'Upcoming', exact: true }).waitFor({ state: 'visible' })
  await page.evaluate(async () => {
    window.scrollTo(0, 0)
    await document.fonts.ready
    const hero = document.querySelector('.home-hero-photo')
    if (hero instanceof HTMLImageElement && !hero.complete) {
      await new Promise(resolve => {
        hero.addEventListener('load', resolve, { once: true })
        hero.addEventListener('error', resolve, { once: true })
      })
    }
  })

  const maskSelectors = [
    { selector: '.home-event-art', reason: 'Event artwork and its overlaid live date' },
    { selector: '.home-story-row > img, .home-story-placeholder', reason: 'Story thumbnail' },
  ]
  const masks = []
  for (const { selector, reason } of maskSelectors) {
    const boxes = await page.locator(selector).evaluateAll(elements => elements.map(element => {
      const rect = element.getBoundingClientRect()
      return { x: rect.x, y: rect.y, right: rect.right, bottom: rect.bottom }
    }))
    for (const box of boxes) {
      const left = Math.max(0, box.x)
      const top = Math.max(0, box.y)
      const right = Math.min(CSS_CLIP.width, box.right)
      const bottom = Math.min(CSS_CLIP.height, box.bottom)
      if (right <= left || bottom <= top) continue
      const x = Math.floor(left * DEVICE_SCALE_FACTOR)
      const y = Math.floor(top * DEVICE_SCALE_FACTOR)
      const x2 = Math.min(WIDTH_PX, Math.ceil(right * DEVICE_SCALE_FACTOR))
      const y2 = Math.min(HEIGHT_PX, Math.ceil(bottom * DEVICE_SCALE_FACTOR))
      masks.push({
        x,
        y,
        width: x2 - x,
        height: y2 - y,
        reason,
        selector,
      })
    }
  }
  if (masks.length === 0) {
    throw new Error('No visible event/story media masks were found; update the test selectors rather than silently comparing live media.')
  }

  await page.screenshot({
    path: screenshotPath,
    type: 'png',
    clip: CSS_CLIP,
    scale: 'device',
    animations: 'disabled',
    caret: 'hide',
  })
  await writeFile(masksPath, `${JSON.stringify({
    width: WIDTH_PX,
    height: HEIGHT_PX,
    deviceScaleFactor: DEVICE_SCALE_FACTOR,
    masks,
  }, null, 2)}\n`)
  console.log(`Captured ${screenshotPath} (${WIDTH_PX}x${HEIGHT_PX}px at DPR ${DEVICE_SCALE_FACTOR})`)
  console.log(`Recorded ${masks.length} live event/story media mask(s) in ${masksPath}`)
  await context.close()
} finally {
  await browser.close()
}
