#!/usr/bin/env node
/**
 * PWA icon generator — pure Node (zlib only), no image dependencies.
 *
 * Paints the brand mark into an RGBA framebuffer: black rounded square, white
 * bitmap "EG" wordmark, one red + one green accent dot (palette rule:
 * black/white + iOS red + iOS green). Drawn at 2x and box-downsampled for
 * antialiasing. Output goes to public/, linked from head + web manifest.
 *
 * Usage: node scripts/make-icons.mjs
 */
import { deflateSync } from 'node:zlib'
import { writeFile, mkdir } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const RED = [255, 59, 48]
const GREEN = [52, 199, 89]
const WHITE = [255, 255, 255]
const BLACK = [0, 0, 0]

/** 5x7 bitmap glyphs for E and G. Same shapes on every platform (see README). */
const GLYPHS = {
  E: ['11111', '10000', '10000', '11110', '10000', '10000', '11111'],
  G: ['01110', '10001', '10000', '10011', '10001', '10001', '01110'],
}

// ── PNG encoding (filter 0 scanlines, RGBA) ──────────────────────────────────

const crcTable = (() => {
  const t = new Uint32Array(256)
  for (let n = 0; n < 256; n++) {
    let c = n
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
    t[n] = c >>> 0
  }
  return t
})()
const crc32 = (buf) => {
  let c = 0xffffffff
  for (const b of buf) c = crcTable[(c ^ b) & 0xff] ^ (c >>> 8)
  return (c ^ 0xffffffff) >>> 0
}

function chunk(type, data) {
  const head = Buffer.alloc(4)
  head.writeUInt32BE(data.length, 0)
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data])
  const crc = Buffer.alloc(4)
  crc.writeUInt32BE(crc32(body), 0)
  return Buffer.concat([head, body, crc])
}

function encodePng(w, h, rgba) {
  const raw = Buffer.alloc((w * 4 + 1) * h)
  for (let y = 0; y < h; y++) {
    raw[y * (w * 4 + 1)] = 0 // filter type 0
    rgba.copy(raw, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4)
  }
  const ihdr = Buffer.alloc(13)
  ihdr.writeUInt32BE(w, 0)
  ihdr.writeUInt32BE(h, 4)
  ihdr[8] = 8 // bit depth
  ihdr[9] = 6 // RGBA
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ])
}

// ── Painting ─────────────────────────────────────────────────────────────────

/** Create a blank RGBA canvas of size×size. */
function canvas(size) {
  return { size, data: new Float64Array(size * size * 4) }
}

function paint(cv, x, y, rgb, alpha = 1) {
  if (x < 0 || y < 0 || x >= cv.size || y >= cv.size) return
  const i = (y * cv.size + x) * 4
  const a = Math.min(1, cv.data[i + 3] + alpha) // blend over (both opaque here)
  cv.data[i] = rgb[0]
  cv.data[i + 1] = rgb[1]
  cv.data[i + 2] = rgb[2]
  cv.data[i + 3] = a
}

function fillRect(cv, x0, y0, w, h, rgb) {
  const x1 = Math.min(cv.size, Math.round(x0 + w))
  const y1 = Math.min(cv.size, Math.round(y0 + h))
  for (let y = Math.max(0, Math.round(y0)); y < y1; y++)
    for (let x = Math.max(0, Math.round(x0)); x < x1; x++) paint(cv, x, y, rgb)
}

function fillCircle(cv, cx, cy, r, rgb) {
  for (let y = Math.max(0, Math.round(cy - r)); y <= Math.min(cv.size - 1, Math.round(cy + r)); y++)
    for (let x = Math.max(0, Math.round(cx - r)); x <= Math.min(cv.size - 1, Math.round(cx + r)); x++)
      if ((x - cx) ** 2 + (y - cy) ** 2 <= r * r) paint(cv, x, y, rgb)
}

function fillRounded(cv, r, rgb) {
  const s = cv.size
  for (let y = 0; y < s; y++)
    for (let x = 0; x < s; x++) {
      // Distance-based rounded-rect test (radius r, full-bleed square).
      const dx = r - Math.min(x, s - 1 - x)
      const dy = r - Math.min(y, s - 1 - y)
      if (Math.max(0, dx) ** 2 + Math.max(0, dy) ** 2 <= r * r) paint(cv, x, y, rgb)
    }
}

/**
 * One icon: black rounded square (or full bleed for maskable/appley), white
 * "EG", red+green dots under the mark. Everything is drawn in the central 80%
 * so maskable circles crop safely.
 */
function drawIcon(size, { maskable = false } = {}) {
  const s = size * 2 // supersample 2x
  const cv = canvas(s)
  if (maskable) fillRect(cv, 0, 0, s, s, BLACK)
  else fillRounded(cv, s * 0.2, BLACK)

  const cell = Math.round(s * 0.058)
  const glyphW = 11 * cell // E(5) + gap(1) + G(5)
  const glyphH = 7 * cell
  const gx = Math.round((s - glyphW) / 2)
  const gy = Math.round((s - glyphH) / 2 - cell * 1.1)
  for (let i = 0; i < 2; i++) {
    const grid = GLYPHS[i === 0 ? 'E' : 'G']
    for (let row = 0; row < 7; row++)
      for (let col = 0; col < 5; col++)
        if (grid[row][col] === '1')
          fillRect(cv, gx + (i * 6 + col) * cell, gy + row * cell, cell + 1, cell + 1, WHITE)
  }
  const dotR = Math.max(2, Math.round(s * 0.032))
  const dotY = gy + glyphH + Math.round(cell * 1.9)
  fillCircle(cv, Math.round(s / 2 - s * 0.075), dotY, dotR, RED)
  fillCircle(cv, Math.round(s / 2 + s * 0.075), dotY, dotR, GREEN)

  // Box-downsample to the target size.
  const out = Buffer.alloc(size * size * 4)
  for (let y = 0; y < size; y++)
    for (let x = 0; x < size; x++) {
      let r = 0, g = 0, b = 0, a = 0
      for (let dy = 0; dy < 2; dy++)
        for (let dx = 0; dx < 2; dx++) {
          const i = ((y * 2 + dy) * s + (x * 2 + dx)) * 4
          r += cv.data[i] / 4
          g += cv.data[i + 1] / 4
          b += cv.data[i + 2] / 4
          a += cv.data[i + 3] / 4
        }
      const o = (y * size + x) * 4
      // Premultiplied-ish straight output; PNG wants straight alpha.
      out[o] = r
      out[o + 1] = g
      out[o + 2] = b
      out[o + 3] = a
    }
  return out
}

async function main() {
  const outDir = path.join(ROOT, 'public')
  await mkdir(path.join(outDir, 'icons'), { recursive: true })
  const jobs = [
    ['../public/favicon.png', 32, {}],
    ['icon-192.png', 192, {}],
    ['icon-512.png', 512, {}],
    ['maskable-512.png', 512, { maskable: true }],
    ['apple-touch-icon.png', 180, {}],
  ]
  for (const [name, size, opts] of jobs)
    await writeFile(
      name.startsWith('../') ? path.join(outDir, 'favicon.png') : path.join(outDir, 'icons', name),
      encodePng(size, size, drawIcon(size, opts))
    )
  console.log('icons: favicon.png + icon-192/512 + maskable-512 + apple-touch-icon')
}

main().catch((err) => {
  console.warn('icons: FAILED —', err && err.message)
  process.exitCode = 1
})
