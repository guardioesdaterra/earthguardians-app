const CACHE = 'earth-guardians-shell-v2'
const CACHE_PREFIX = 'earth-guardians-shell-'
const SCOPE_URL = new URL('./', self.registration.scope).toString()

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.add(SCOPE_URL)).catch(() => undefined))
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(caches.keys().then((keys) => Promise.all(keys
    .filter((key) => key.startsWith(CACHE_PREFIX) && key !== CACHE)
    .map((key) => caches.delete(key)))))
  self.clients.claim()
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  if (request.method !== 'GET') return
  const url = new URL(request.url)
  if (url.origin !== self.location.origin) return

  const isSnapshot = url.pathname.endsWith('/eg-cache/eg_feed_cache.json') || url.pathname.endsWith('/eg-cache/eg_maps_cache.json')
  if (isSnapshot) {
    event.respondWith(fetch(request).then((response) => {
      if (response.ok) void caches.open(CACHE).then((cache) => cache.put(request, response.clone()))
      return response
    }).catch(async () => (await caches.match(request)) || Response.error()))
    return
  }

  if (request.mode === 'navigate') {
    event.respondWith(fetch(request).then((response) => {
      if (response.ok) void caches.open(CACHE).then((cache) => cache.put(request, response.clone()))
      return response
    }).catch(async () => (await caches.match(request)) || (await caches.match(SCOPE_URL)) || Response.error()))
    return
  }

  event.respondWith(caches.match(request).then((cached) => cached || fetch(request).then((response) => {
    if (response.ok) void caches.open(CACHE).then((cache) => cache.put(request, response.clone()))
    return response
  })))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const scope = new URL('./', self.registration.scope)
  const requested = event.notification.data?.href || scope.href
  const target = new URL(requested, scope)
  if (target.origin !== self.location.origin || !target.pathname.startsWith(scope.pathname)) return

  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
    for (const client of windows) {
      if (!client.url.startsWith(scope.href)) continue
      const focused = 'navigate' in client ? await client.navigate(target.href) : client
      return (focused || client).focus()
    }
    return self.clients.openWindow(target.href)
  })())
})
