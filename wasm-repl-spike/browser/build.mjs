// Shared compile machinery for the browser loop: the stdlib base is one
// wasm (emitted once into the build dir — the output phase skips it
// thereafter; byte-identical across edits, so the page instantiates it
// once per session), and every edit compiles a FILE-GRANULAR delta —
// only the IC-modified files are defined, clean files' declarations
// import from the live patch chain under the app's own module name (the
// multimodule `__fn$`/`__vt$`/`__rt$` convention is the ABI; design in
// ../docs/DESIGN.md §7). STABLE identities and file names matter: identity
// keys the wasm declarations, and the IC cache's config hash covers the
// link's -ir-output-name — a fresh cache means re-lowering the stdlib.
//
// The registry ferry: read the last patch's custom section (readRegistry
// from glue.mjs — the one parser), inject __hotRegistry into the next
// compile's own always-dirty file (the image is the previous-state
// store).

import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { CompileServer } from './compile-server.mjs'
import { readRegistry } from './glue.mjs'

export const HERE = path.dirname(new URL(import.meta.url).pathname)
export const KOTLIN_DIST = process.env.KOTLIN_DIST || path.join(HERE, '..', '..', 'dist')
const KOTLINC = path.join(KOTLIN_DIST, 'kotlinc', 'bin', 'kotlinc-wasm')
const STDLIB = path.join(KOTLIN_DIST, 'kotlinc', 'lib', 'kotlin-stdlib-wasm-js.klib')

// resident-JVM compile server (the easy latency win: kills the ~2x kotlinc
// JVM spawn per edit); falls back to spawning kotlinc if it dies
const server = new CompileServer()
export const BUILD = path.join(HERE, 'app', 'build')
const IC_CACHE = path.join(BUILD, 'ic-cache')

// multimodule output names: the app module lands as _<name>_.wasm, the
// stdlib base as kotlin-kotlin-stdlib.wasm with its generated glue
const APP_OUTPUT = 'appimg'
const APP_WASM = `_${APP_OUTPUT}_.wasm`
export const APP_PATCH_WASM = APP_WASM
export const BASE_WASM = 'kotlin-kotlin-stdlib.wasm'
export const BASE_PATH = path.join(BUILD, BASE_WASM)

fs.mkdirSync(BUILD, { recursive: true })

/** A fresh IMAGE chain needs a FULL first patch: the IC cache's
 * "unchanged" is file-content truth, not image truth — it survives a
 * dev-server or page restart, the live instance chain does not, so a
 * file never edited in the previous session would come out "clean" and
 * be emitted as an IMPORT against a chain that doesn't exist. Forcing
 * the app files dirty is the fix; wiping the APP's IC entries is cheap
 * (per-file wasm fragments of a few sources), and the stdlib's entries
 * are kept (re-lowering the stdlib is the expensive part of a cold
 * boot). The production dev task owns this contract: full-first-patch
 * for every new image. */
export function forceFullPatch() {
    if (!fs.existsSync(IC_CACHE)) return
    for (const entry of fs.readdirSync(IC_CACHE, { withFileTypes: true })) {
        if (!entry.isDirectory()) continue
        for (const klib of fs.readdirSync(path.join(IC_CACHE, entry.name))) {
            if (!klib.startsWith('kotlin-')) {
                fs.rmSync(path.join(IC_CACHE, entry.name, klib), { recursive: true })
            }
        }
    }
}

let version = 0
let lastPatchBytes = null

// the app's source files, compiled as ONE module (stable names across
// edits — identity and the IC cache both key on them). Each file is a
// language-feature story: model = state/migration, zoo = dispatch,
// lambdas = the Function machinery + cross-file inline, shapes =
// interface default methods on live instances, state = objects and
// companions, toolbox = generics/inline/vararg/extension.
export const SRC_NAMES = ['model.kt', 'zoo.kt', 'lambdas.kt', 'shapes.kt', 'state.kt', 'toolbox.kt']

/** Compile `sources` ({ 'model.kt': text, 'zoo.kt': text }) as the next
 * PATCH (app module only, hot); the stdlib base emits only if absent
 * from the build dir (byte-identical across edits, so it only needs
 * loading once). The registry of the previous patch is ferried
 * automatically — into its own always-dirty file (it is the
 * previous-state channel; its payload is one object + one const).
 * Only CHANGED files are rewritten on disk so identical content never
 * dirties the IC. The FIRST compile of a process forces a full patch
 * (see forceFullPatch). Returns { version, bytes, error }. ASYNC:
 * talks to the resident compile server. */
export async function compileNext(sources) {
    if (version === 0) forceFullPatch()
    version += 1
    let reg = ''
    if (lastPatchBytes) {
        const blob = readRegistry(lastPatchBytes)
        if (blob) {
            reg = `internal object __hotRegistry { const val REGISTRY = "${Buffer.from(blob).toString('base64')}" }\n`
        }
    }
    const files = []
    for (const name of SRC_NAMES) {
        const text = sources[name]
        if (text === undefined) throw new Error(`compileNext: no source for ${name}`)
        const disk = path.join(BUILD, name)
        // write only on change: the IC's per-file fingerprints are the
        // delta link's dirty set — identical rewrites must not dirty
        let current = null
        try { current = fs.readFileSync(disk, 'utf-8') } catch { /* absent */ }
        if (current !== text) fs.writeFileSync(disk, text)
        files.push(disk)
    }
    fs.writeFileSync(path.join(BUILD, 'registry.kt'), reg)
    files.push(path.join(BUILD, 'registry.kt'))
    const stepArgs = [
        ['-Xwasm-target=wasm-js', ...files, '-ir-output-dir', BUILD,
         '-ir-output-name', APP_OUTPUT, '-libraries', STDLIB, '-Xwasm-hot-reload',
         '-Xwasm-generate-closed-world-multimodule', `-Xcache-directory=${IC_CACHE}`],
        ['-Xwasm-target=wasm-js', `-Xinclude=${path.join(BUILD, `${APP_OUTPUT}.klib`)}`, '-libraries', STDLIB,
         '-ir-output-dir', BUILD, '-ir-output-name', APP_OUTPUT, '-main', 'noCall',
         '-Xwasm-hot-reload', '-Xwasm-generate-closed-world-multimodule', `-Xcache-directory=${IC_CACHE}`],
    ]
    for (const args of stepArgs) {
        // the resident server first; the spawn fallback keeps the loop
        // working (just slower) if the server can't start or dies
        let result = await server.compile(args)
        if (result === null) {
            const r = spawnSync(KOTLINC, args, { encoding: 'utf-8' })
            result = [r.status === 0, (r.stdout + r.stderr).trim()]
        }
        if (!result[0]) {
            version -= 1
            return { version, error: result[1].trim() }
        }
    }
    lastPatchBytes = fs.readFileSync(path.join(BUILD, APP_WASM))
    return { version, bytes: lastPatchBytes, error: null }
}

/** The stdlib base wasm bytes (emitted by the same link; identical across
 * patches, so it is loaded once per session). */
export function baseBytes() {
    return fs.readFileSync(BASE_PATH)
}
