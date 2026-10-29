#!/usr/bin/env node
/**
 * Copy the public map snapshots shared with the native apps into the PWA bundle.
 * These are read-only public Crew and Project Grant datasets; the manager portal
 * and its server-side access controls are intentionally not included.
 */
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const WEB_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const REPO_ROOT = path.resolve(WEB_ROOT, '..')
const ANDROID_DIR = path.join(REPO_ROOT, 'app/src/main/assets/maps')
const IOS_DIR = path.join(REPO_ROOT, 'ios/EarthGuardians/Resources/Maps')
const files = [
  ['regions', 'crew-regions.json'],
  ['crews', 'crew-locations.json'],
  ['grants', 'project-grants.json'],
]

async function readRows(directory, filename) {
  const parsed = JSON.parse(await readFile(path.join(directory, filename), 'utf8'))
  if (!Array.isArray(parsed)) throw new Error(`${path.relative(REPO_ROOT, path.join(directory, filename))} must contain a JSON array.`)
  return parsed
}

const snapshot = { schemaVersion: 1 }
for (const [key, filename] of files) {
  const androidRows = await readRows(ANDROID_DIR, filename)
  const iosRows = await readRows(IOS_DIR, filename)
  if (JSON.stringify(androidRows) !== JSON.stringify(iosRows)) {
    throw new Error(`Android/iOS public map data drift detected in ${filename}; align the native snapshot source before generating the PWA bundle.`)
  }
  snapshot[key] = androidRows
}

const serialized = `${JSON.stringify(snapshot)}\n`
for (const directory of ['cache', 'public/eg-cache']) {
  const targetDirectory = path.join(WEB_ROOT, directory)
  await mkdir(targetDirectory, { recursive: true })
  await writeFile(path.join(targetDirectory, 'eg_maps_cache.json'), serialized, 'utf8')
}
console.log(`maps: synced ${snapshot.regions.length} regions, ${snapshot.crews.length} crews, ${snapshot.grants.length} public grants; Android/iOS snapshots match`)
