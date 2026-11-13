#!/usr/bin/env node
// The dev server: serves the page, watches app/src/*.kt, recompiles on save
// (whole module, -Xwasm-hot-reload), and bumps the version the page polls.
// The registry ferry, section parsing and compile steps live in build.mjs
// (shared with the Node gate, test.mjs).
//
//   node dev.mjs            # http://localhost:8123 — then edit the files in app/src/
//
// What you should see: the counter's state survives every save. Body
// edits swap instantly; a field edit migrates the live Model instance in
// place (new fields read type defaults on the migrated instance — the
// initform follow-up). A compile error keeps the app running the last
// good version and shows the error; an edit the ENGINE rejects (e.g. a
// method signature change) throws at the setter — the page overlay
// shows it and the old code keeps running.

import fs from 'node:fs'
import http from 'node:http'
import path from 'node:path'
import { HERE, BUILD, compileNext, APP_PATCH_WASM, SRC_NAMES, forceFullPatch } from './build.mjs'

const SRC_DIR = path.join(HERE, 'app', 'src')
const PORT = Number(process.env.PORT || 8123)

let version = 0
let lastError = null
// set when a FRESH page reports itself (its instance chain is empty): the
// next rebuild must produce a FULL patch — a delta would import from
// instances the new page doesn't hold (see build.mjs forceFullPatch)
let forceFullNext = false

let building = false
let pending = false

// one compile at a time (the IC cache and the version counter must not
// interleave); a save during a compile queues exactly one trailing rebuild
async function rebuild() {
    if (building) {
        pending = true
        return
    }
    building = true
    try {
        if (forceFullNext) {
            forceFullPatch()
            forceFullNext = false
        }
        const result = await compileNext(readSources())
        version = result.version
        lastError = result.error
        if (result.error) {
            console.error('[dev] compile failed:\n' + result.error)
        } else {
            // serve the patch under a versioned name; the base has a stable
            // name (byte-identical across compiles, loaded once by the page)
            fs.copyFileSync(path.join(BUILD, APP_PATCH_WASM), path.join(BUILD, `app-v${version}.wasm`))
            console.log(`[dev] v${version} served (${result.bytes.length} B patch; base unchanged)`)
        }
    } finally {
        building = false
        if (pending) {
            pending = false
            rebuild()
        }
    }
}

function readSources() {
    const sources = {}
    for (const name of SRC_NAMES) sources[name] = fs.readFileSync(path.join(SRC_DIR, name), 'utf-8')
    return sources
}

rebuild()
for (const name of SRC_NAMES) {
    fs.watchFile(path.join(SRC_DIR, name), { interval: 250 }, () => {
        console.log(`[dev] ${name} changed — recompiling`)
        rebuild()
    })
}

const MIME = {
    '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript',
    '.wasm': 'application/wasm', '.json': 'application/json',
}

http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost')
    if (url.pathname === '/version') {
        if (url.searchParams.has('boot') && !forceFullNext) {
            forceFullNext = true
            rebuild()
        }
        res.writeHead(200, { 'content-type': 'application/json' })
        res.end(JSON.stringify({ version, error: lastError }))
        return
    }
    const file = url.pathname === '/' ? '/index.html' : url.pathname
    const disk = path.join(HERE, file)
    if (!path.normalize(disk).startsWith(HERE) || !fs.existsSync(disk) || !fs.statSync(disk).isFile()) {
        res.writeHead(404)
        res.end('not found')
        return
    }
    res.writeHead(200, { 'content-type': MIME[path.extname(disk)] || 'application/octet-stream' })
    res.end(fs.readFileSync(disk))
}).listen(PORT, () => {
    console.log(`[dev] http://localhost:${PORT} — edit an app/src file and save`)
})
