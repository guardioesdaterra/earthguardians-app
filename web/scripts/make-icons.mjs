#!/usr/bin/env node
/**
 * Delegate web icon generation to the shared Pillow pipeline so the PWA uses
 * the official Earth Guardians mark shared with Android and iOS.
 */
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const script = path.join(path.dirname(fileURLToPath(import.meta.url)), 'make-icons.py')
const python = process.env.PYTHON || 'python3'
const result = spawnSync(python, [script, '--web-only'], { stdio: 'inherit' })
if (result.error) {
  console.error(`icons: could not start ${python}: ${result.error.message}`)
  process.exit(1)
}
process.exit(result.status ?? 1)
