import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join, resolve } from 'node:path'

const [outputArg, baseArg] = process.argv.slice(2)
if (!outputArg || !baseArg) {
  console.error('Usage: node scripts/check-generated-site.mjs <public-dir> <base-url>')
  process.exit(2)
}

const outputDir = resolve(outputArg)
const baseUrl = baseArg.endsWith('/') ? baseArg : `${baseArg}/`
const routeFiles = [
  'index.html',
  'events/index.html',
  'stories/index.html',
  'maps/index.html',
  'more/index.html',
  'settings/index.html',
  'notifications/index.html',
]
const staticFiles = [
  'manifest.webmanifest',
  'sw.js',
  'icons/apple-touch-icon.png',
  'icons/icon-192.png',
  'icons/icon-512.png',
  'icons/maskable-512.png',
  'eg-cache/eg_feed_cache.json',
  'eg-cache/eg_sitemap_cache.json',
]

for (const relativePath of [...routeFiles, ...staticFiles]) {
  assert.ok(existsSync(join(outputDir, relativePath)), `missing generated file: ${relativePath}`)
}

const homeHtml = readFileSync(join(outputDir, 'index.html'), 'utf8')
assert.ok(
  homeHtml.includes(`href="${baseUrl}manifest.webmanifest"`),
  `home page must link to the manifest under base URL ${baseUrl}`,
)

const manifest = JSON.parse(readFileSync(join(outputDir, 'manifest.webmanifest'), 'utf8'))
assert.equal(typeof manifest.name, 'string', 'manifest must declare an app name')
const pagesOrigin = new URL(baseUrl, 'https://pages.invalid')
const pagesBasePath = pagesOrigin.pathname
assert.equal(
  new URL(manifest.start_url, pagesOrigin).pathname,
  pagesBasePath,
  'manifest start_url must resolve inside the Pages base URL',
)
assert.equal(
  new URL(manifest.scope, pagesOrigin).pathname,
  pagesBasePath,
  'manifest scope must resolve inside the Pages base URL',
)
assert.ok(Array.isArray(manifest.icons) && manifest.icons.length > 0, 'manifest must declare icons')
assert.ok(manifest.icons.some((icon) => icon.sizes === '192x192'), 'manifest must declare a 192px icon')
assert.ok(manifest.icons.some((icon) => icon.sizes === '512x512'), 'manifest must declare a 512px icon')
for (const icon of manifest.icons) {
  const iconUrl = new URL(icon.src, pagesOrigin)
  assert.ok(iconUrl.pathname.startsWith(pagesBasePath), `icon escapes Pages base URL: ${icon.src}`)
  const relativeIconPath = decodeURIComponent(iconUrl.pathname.slice(pagesBasePath.length))
  assert.ok(existsSync(join(outputDir, relativeIconPath)), `missing generated manifest icon: ${relativeIconPath}`)
}

console.log(`PWA/GitHub Pages output OK: ${routeFiles.length} routes, manifest and static resources at ${baseUrl}`)
