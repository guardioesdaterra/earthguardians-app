#!/usr/bin/env node
/**
 * Build-time feed harvester — the web twin of Android's `ContentRepository`.
 *
 * Downloads the three site sources with the same User-Agent contract as the
 * native apps and writes the exact same JSON shape the Android app keeps on
 * disk (eg_feed_cache.json / eg_sitemap_cache.json), so the schema stays
 * platform-neutral:
 *
 *   { fetchedAt: <ms>, feed: { events: EgEvent[], posts: EgPost[] } }
 *   { fetchedAt: <ms>, urls: string[] }
 *
 * Runs before `nuxt generate` on CI (dependency-free on purpose — mirrors the
 * Kotlin XmlPullParser strategy: regex over the feed body, namespace-agnostic).
 * If the network is unavailable it leaves the committed cache untouched: the
 * site is offline-first, exactly like the app — a stale bake beats no build.
 *
 * Usage: node scripts/fetch-feeds.mjs
 */
import { writeFile } from 'node:fs/promises'
import { mkdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const SITE = 'https://www.earthguardians.org'
const UA = 'EG-App/1.0 (+https://www.earthguardians.org)'
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function fetchText(url, timeoutMs = 60_000) {
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(new Error('timeout')), timeoutMs)
  try {
    const res = await fetch(url, {
      headers: { 'User-Agent': UA, Accept: 'application/xml, text/html;q=0.5, */*;q=0.3' },
      signal: ctrl.signal,
    })
    if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`)
    return await res.text()
  } finally {
    clearTimeout(timer)
  }
}

// ── XML string helpers (keep in lockstep with the Kotlin/Python twins) ────────

const CDATA = /^\s*<!\[CDATA\[([\s\S]*?)\]\]>\s*$/
const unwrap = (v) => {
  if (!v) return ''
  const m = CDATA.exec(v)
  return (m ? m[1] : v).trim()
}

/** Entity map — same set as Kotlin's EgText.fromHtml/decodeFeedEntities. */
const ENTITIES = {
  '&amp;': '&', '&lt;': '<', '&gt;': '>', '&quot;': '"', '&apos;': '',
  '&#39;': "'", '&nbsp;': ' ',
}
function decodeEntities(t) {
  return (t || '')
    .replace(/&(amp|lt|gt|quot|apos|nbsp);|\&#39;/g, (e) => ENTITIES[e] ?? ENTITIES['&' + e.slice(1, -1)] ?? e)
    .replace(/&#39;/g, "'")
}
function down(t) {
  return decodeEntities((t || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ')).trim()
}
const titleFromSlug = (slug) =>
  (slug || '')
    .replace(/[-_|]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .split(' ')
    .map((w) => (w ? w[0].toUpperCase() + w.slice(1) : w))
    .join(' ')

function field(body, name) {
  const re = new RegExp(`<(?:[\\w.-]+:)?${name}(?:\\s[^>]*)?>([\\s\\S]*?)</(?:[\\w.-]+:)?${name}>`)
  const m = re.exec(body)
  return m ? unwrap(m[1]) : null
}

function image(body) {
  const url = (re) => {
    const m = re.exec(body)
    return m ? decodeEntities(m[1]) : null
  }
  return (
    url(/<media:thumbnail[^>]*\burl="([^"]+)"/i) ??
    url(/<media:content[^>]*\burl="([^"]+)"/i) ??
    url(/<enclosure[^>]*\burl="([^"]+)"/i)
  )
}

/** RFC-822 pubDate → epoch ms (null when absent/unparseable). */
function parsePubDate(raw) {
  if (!raw) return null
  const t = Date.parse(raw.replace(/\s+/g, ' ').trim())
  return Number.isFinite(t) ? t : null
}

/** /events/{yyyy}/{M}/{d}/slug → epoch ms of that calendar date at noon UTC. */
function dateFromEventSlug(link) {
  const m = /\/events\/(\d{4})\/(\d{1,2})\/(\d{1,2})\//.exec(link)
  if (!m) return null
  const [, y, mo, d] = m
  const t = new Date(`${y}-${mo.padStart(2, '0')}-${d.padStart(2, '0')}T12:00:00Z`).getTime()
  return Number.isFinite(t) ? t : null
}

/** Mirrors ContentRepository mapItem semantics for both collections at once. */
function parseItems(xml, isEvent) {
  const out = []
  const itemRe = /<item(?:\s[^>]*)?>([\s\S]*?)<\/item>/g
  let m
  while ((m = itemRe.exec(xml))) {
    const body = m[1]
    const link = down(field(body, 'link') || '')
    if (!link.startsWith('http')) continue
    if (isEvent ? !link.includes('/events/') : !link.includes('/earthguardian-blog/')) continue

    let title = down(field(body, 'title') || '')
    if (!title) title = titleFromSlug(link.split(/[/?]/).filter(Boolean).pop())
    const descRaw = field(body, 'description') || ''
    const encoded = field(body, 'encoded') // <content:encoded>
    const pub = parsePubDate(field(body, 'pubDate') || '')

    if (isEvent) {
      out.push({
        url: link,
        title,
        imageUrl: image(body),
        startsAt: pub ?? dateFromEventSlug(link),
        location: field(body, 'category') || null,
        description: down(descRaw) || null,
      })
    } else {
      // Content HTML: content:encoded when the site provides it, else the rich
      // description CDATA (Squarespace sometimes ships the article only once).
      const contentHtml = encoded || descRaw
      out.push({
        url: link,
        title,
        imageUrl: image(body),
        publishedAt: pub,
        teaser: down(descRaw) || null,
        contentHtml: contentHtml || null,
      })
    }
  }
  return out
}

function parseSitemap(xml) {
  const urls = []
  const re = /<loc>([\s\S]*?)<\/loc>/g
  let m
  while ((m = re.exec(xml))) {
    const u = decodeEntities(m[1]).trim()
    if (u.startsWith('http')) urls.push(u)
  }
  return urls
}

// ── Main ──────────────────────────────────────────────────────────────────────

async function main() {
  const targets = [
    { name: 'blog', url: `${SITE}/earthguardian-blog?format=RSS` },
    { name: 'events', url: `${SITE}/events?format=RSS` },
    { name: 'sitemap', url: `${SITE}/sitemap.xml` },
  ]
  const results = {}
  let failures = 0
  for (const t of targets) {
    try {
      results[t.name] = await fetchText(t.url)
      console.log(`feeds: ${t.name} ok (${results[t.name].length} bytes)`)
    } catch (err) {
      console.warn(`feeds: ${t.name} FAILED — ${err.message || err}`)
      failures++
    }
    await sleep(800) // be gentle with the Squarespace origin
  }

  if (results.blog || results.events) {
    const events = results.events ? parseItems(results.events, true) : []
    const posts = results.blog ? parseItems(results.blog, false) : []
    const feed = { fetchedAt: Date.now(), feed: { events, posts } }
    for (const dir of ['cache', 'public/eg-cache']) {
      const dest = path.join(ROOT, dir)
      mkdirSync(dest, { recursive: true })
      await writeFile(path.join(dest, 'eg_feed_cache.json'), JSON.stringify(feed), 'utf8')
    }
    console.log(`feeds: wrote ${events.length} events, ${posts.length} posts`)
  }
  if (results.sitemap) {
    const sm = { fetchedAt: Date.now(), urls: parseSitemap(results.sitemap) }
    for (const dir of ['cache', 'public/eg-cache']) {
      const dest = path.join(ROOT, dir, 'eg_sitemap_cache.json')
      mkdirSync(path.dirname(dest), { recursive: true })
      await writeFile(dest, JSON.stringify(sm), 'utf8')
    }
    console.log(`feeds: wrote ${sm.urls.length} sitemap urls`)
  }

  if (failures === targets.length) {
    console.warn('feeds: every source failed — keeping committed cache (offline-first)')
  }
}

main().catch((err) => {
  // Stores always keep the committed cache on failure — never fail the build.
  console.warn('feeds: aborted —', err && err.message)
})
