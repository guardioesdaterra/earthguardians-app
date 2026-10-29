import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const webRoot = join(dirname(fileURLToPath(import.meta.url)), '..')
const config = readFileSync(join(webRoot, 'nuxt.config.ts'), 'utf8')
const packageJson = JSON.parse(readFileSync(join(webRoot, 'package.json'), 'utf8'))
const expectedRoutes = [
  '/',
  '/events',
  '/stories',
  '/maps',
  '/more',
  '/settings',
  '/notifications',
  '/manifest.webmanifest',
]

test('Nuxt build script and GitHub Pages base path remain configurable', () => {
  assert.equal(packageJson.scripts.generate, 'nuxi generate')
  assert.match(config, /const BASE = process\.env\.NUXT_APP_BASE_URL \|\| '\/'/)
  assert.match(config, /baseURL: BASE/)
  assert.match(config, /href: `\$\{BASE\}manifest\.webmanifest`/)
  assert.match(config, /href: `\$\{BASE\}icons\/apple-touch-icon\.png`/)
})

test('Nuxt prerender list keeps the PWA and app routes needed on GitHub Pages', () => {
  const prerenderBlock = config.match(/prerender:\s*\{([\s\S]*?)\n\s*\},/)
  assert.ok(prerenderBlock, 'Nuxt prerender config must exist')
  assert.match(prerenderBlock[1], /\bcrawlLinks\s*:\s*false\b/, 'SSG must not crawl nested external deep-link URLs')
  const routeBlock = prerenderBlock[1].match(/routes:\s*\[([\s\S]*?)\]/)
  assert.ok(routeBlock, 'Nuxt prerender route list must exist')
  const routes = [...routeBlock[1].matchAll(/'([^']+)'/g)].map((match) => match[1])
  assert.deepEqual(routes, expectedRoutes)
})

test('offline cache fixtures and PWA icons are present and valid', () => {
  const requiredFiles = [
    'public/eg-cache/eg_feed_cache.json',
    'public/eg-cache/eg_sitemap_cache.json',
    'public/icons/apple-touch-icon.png',
    'public/icons/icon-192.png',
    'public/icons/icon-512.png',
    'public/icons/maskable-512.png',
  ]
  for (const relativePath of requiredFiles) {
    assert.ok(existsSync(join(webRoot, relativePath)), `missing ${relativePath}`)
  }
  for (const relativePath of requiredFiles.filter((path) => path.endsWith('.json'))) {
    assert.doesNotThrow(() => JSON.parse(readFileSync(join(webRoot, relativePath), 'utf8')))
  }
})
