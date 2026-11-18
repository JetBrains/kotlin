// A resident-JVM compile shim for the browser loop. The shim (lrepl's
// CompileServer.kt, built into the sibling lrepl/server.jar) forwards each
// request to the official Kotlin compile daemon — the same resident
// service Gradle drives, which already speaks the wasm target — so the
// warm compiler JVM is upstream's, spawned and version-checked by its own
// client library (a dist rebuild restarts it automatically; no more
// delete-server.jar-after-dist rule). Protocol: one request per line,
// arguments joined with \x01; one response line
// "RESULT <rc> <base64 diagnostics>".
//
// If the shim dies or the jar can't be built, compile() returns null and
// the caller falls back to spawning kotlinc — the loop keeps working,
// just slower.

import { spawn, spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const HERE = path.dirname(new URL(import.meta.url).pathname)
const KOTLIN_DIST = process.env.KOTLIN_DIST || path.join(HERE, '..', '..', 'dist')
const KOTLINC = path.join(KOTLIN_DIST, 'kotlinc', 'bin', 'kotlinc')
const COMPILER_JAR = path.join(KOTLIN_DIST, 'kotlinc', 'lib', 'kotlin-compiler.jar')
const DAEMON_CLIENT_JAR = path.join(KOTLIN_DIST, 'kotlinc', 'lib', 'kotlin-daemon-client.jar')
const SHIM_CP = `${COMPILER_JAR}:${DAEMON_CLIENT_JAR}`
const SERVER_JAR = path.join(HERE, '..', 'lrepl', 'server.jar')
const SERVER_SRC = path.join(HERE, '..', 'lrepl', 'CompileServer.kt')

const US = '\x01'

export class CompileServer {
    constructor() {
        this.proc = null
        this.log = null
    }

    ensureJar() {
        if (fs.existsSync(SERVER_JAR)) return true
        const r = spawnSync(
            KOTLINC, ['-cp', SHIM_CP, SERVER_SRC, '-d', SERVER_JAR],
            { encoding: 'utf-8' },
        )
        return r.status === 0
    }

    ensureRunning() {
        if (this.proc && this.proc.exitCode === null) return true
        if (!this.ensureJar()) return false
        this.proc = spawn(
            'java', ['-cp', `${SERVER_JAR}:${SHIM_CP}`, 'CompileServerKt'],
            { stdio: ['pipe', 'pipe', 'inherit'] },
        )
        // no startup banner — the server answers request-for-request
        return true
    }

    /** (ok, diagnostics) or null → the caller falls back to spawning.
     *  timeoutMs: warm compiles take seconds, so the default is tight;
     *  pass a long one for the rare legitimately-slow compile (a wiped
     *  IC cache re-lowers the whole stdlib). */
    async compile(args, timeoutMs = 180_000) {
        if (!this.ensureRunning()) return null
        try {
            this.proc.stdin.write(args.join(US) + '\n')
            // WATCHDOG: the shim is an RMI client of the compile daemon,
            // and RMI has NO timeout — a daemon death mid-request leaves
            // the shim blocked forever (the classic wedge: an aborted run
            // + a dist rebuild). Bound it; on timeout the catch below
            // kills the shim and the caller falls back to spawning
            // kotlinc, so the loop always makes progress.
            const line = await Promise.race([
                this.readLine(),
                new Promise((_, reject) => {
                    this.timeout = setTimeout(
                        () => reject(new Error(`compile server timed out (${Math.round(timeoutMs / 1000)}s)`)),
                        timeoutMs,
                    )
                }),
            ])
            clearTimeout(this.timeout)
            const m = /^RESULT (\d+) (.*)$/.exec(line)
            if (!m) throw new Error('bad server response: ' + line.slice(0, 80))
            const diag = Buffer.from(m[2], 'base64').toString()
            return [Number(m[1]) === 0, diag]
        } catch (e) {
            clearTimeout(this.timeout)
            try { this.proc.kill() } catch { /* already gone */ }
            this.proc = null
            console.error('[server] fell back to CLI:', String(e).slice(0, 120))
            return null
        }
    }

    /** Read one \n-terminated line via a persistent line buffer. Robust to
     *  the server dying at any point: an exit fires the pending waiters
     *  AND flags the server dead, so a waiter pushed after the exit (the
     *  race) rejects immediately instead of hanging forever — compile()
     *  then takes the spawn fallback and the loop keeps working. */
    readLine() {
        if (this.buf === undefined) {
            this.buf = ''
            this.waiters = []
            this.dead = this.proc.exitCode !== null
            const failAll = (e) => {
                this.dead = true
                const waiters = this.waiters
                this.waiters = []
                waiters.forEach((w) => w.reject(e))
            }
            this.proc.stdout.on('data', (chunk) => {
                this.buf += chunk
                this.drain()
            })
            this.proc.stdout.on('error', failAll)
            this.proc.on('exit', () => failAll(new Error('compile server exited')))
        }
        return new Promise((resolve, reject) => {
            if (this.dead && this.buf.indexOf('\n') === -1) {
                reject(new Error('compile server exited'))
                return
            }
            this.waiters.push({ resolve, reject })
            this.drain()
        })
    }

    drain() {
        if (this.waiters.length === 0) return
        const i = this.buf.indexOf('\n')
        if (i === -1) return
        const line = this.buf.slice(0, i)
        this.buf = this.buf.slice(i + 1)
        const w = this.waiters.shift()
        w.resolve(line)
    }
}
