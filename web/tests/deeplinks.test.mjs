import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import test from 'node:test'
import ts from 'typescript'

// Compile only this dependency-free utility in memory so Node 20 can test the
// TypeScript source directly without a bundler, generated files, or local build.
const sourcePath = fileURLToPath(new URL('../utils/deeplinks.ts', import.meta.url))
const source = await readFile(sourcePath, 'utf8')
const { outputText } = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
})
const moduleURL = `data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`
const { parseAppRoute } = await import(moduleURL)

const pageURL = 'https://www.earthguardians.org/programs/youth%20climate?source=app&topic=climate+justice#join'

for (const [name, input] of [
  ['custom scheme', `earthguardians://section?source=share&u=${pageURL}`],
  ['page query', `/?page=section&u=${pageURL}`],
  ['route query', `/?route=section&u=${pageURL}`],
]) {
  test(`preserves nested query and fragment from raw ${name} arguments`, () => {
    assert.deepEqual(parseAppRoute(input), { screen: 'section', argument: pageURL })
  })
}

test('preserves a percent-encoded nested page argument', () => {
  const input = `/?page=section&u=${encodeURIComponent(pageURL)}`
  assert.deepEqual(parseAppRoute(input), { screen: 'section', argument: pageURL })
})
