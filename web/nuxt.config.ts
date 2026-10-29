// Earth Guardians web — Nuxt SSG for GitHub Pages.
// No external modules: everything (PWA, theme, design system) is hand-rolled so
// the build never depends on a module ecosystem update breaking CI.
const rawBase = process.env.NUXT_APP_BASE_URL || '/'
const BASE = rawBase.endsWith('/') ? rawBase : `${rawBase}/`

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: false },
  typescript: { typeCheck: false },
  app: {
    baseURL: BASE,
    // SSG pages become SPA navigations on GH Pages via _payload.json, no router
    // mode needed.
    pageTransition: { name: 'page', mode: 'out-in' },
    head: {
      htmlAttrs: { lang: 'en' },
      title: 'Earth Guardians',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1, viewport-fit=cover' },
        {
          name: 'description',
          content:
            'Earth Guardians is a revolutionary environmental nonprofit run for youth, by youth. Events, stories, EG-Maps and every program of the movement — offline-first.',
        },
        // Brand rule: black/white + iOS red + iOS green only.
        { name: 'theme-color', media: '(prefers-color-scheme: light)', content: '#FFFFFF' },
        { name: 'theme-color', media: '(prefers-color-scheme: dark)', content: '#000000' },
        { name: 'apple-mobile-web-app-capable', content: 'yes' },
        { name: 'apple-mobile-web-app-status-bar-style', content: 'black-translucent' },
        { name: 'apple-mobile-web-app-title', content: 'Earth Guardians' },
        { property: 'og:title', content: 'Earth Guardians' },
        { property: 'og:description', content: 'A global climate movement, run for youth by youth. Events, stories, maps and every program — in your pocket.' },
        { property: 'og:site_name', content: 'Earth Guardians' },
        { name: 'mobile-web-app-capable', content: 'yes' },
      ],
      link: [
        { rel: 'icon', type: 'image/png', href: `${BASE}favicon.png` },
        { rel: 'apple-touch-icon', href: `${BASE}icons/apple-touch-icon.png` },
        { rel: 'manifest', href: `${BASE}manifest.webmanifest` },
      ],
      script: [
        {
          // Resolve the theme before first paint (same storage key as useTheme).
          innerHTML:
            "try{var m=localStorage.getItem('eg_theme')||'system';var d=m==='dark'||(m!=='light'&&matchMedia('(prefers-color-scheme: dark)').matches);document.documentElement.dataset.theme=d?'dark':'light'}catch(e){}",
        },
      ],
    },
  },
  css: ['~/assets/css/main.css'],
  nitro: {
    prerender: {
      // All app pages are listed below; crawling query links can treat nested
      // external deep-link URLs as prerender cache keys.
      crawlLinks: false,
      routes: ['/', '/events', '/stories', '/maps', '/more', '/settings', '/notifications', '/manifest.webmanifest'],
      ignore: ['/api'],
    },
  },
})
