// A resident-JVM compile server for the browser loop — the port of the
// REPL's CompileServer pattern (the easy latency win: the measured 3.1 s
// per edit is dominated by ~2× kotlinc JVM spawn; a resident JVM looping
// `CLICompiler.doMainNoExit` per request makes each step ~0.1–0.5 s warm).
//
// The server itself is lrepl's CompileServer.kt, built into the sibling
// lrepl/server.jar on first use (built from this dist's kotlinc — it must
// be rebuilt when the dist changes; the jar is deleted then, same
// operational rule as the REPL). Protocol: one request per line, arguments
// joined with \x01; one response line "RESULT <rc> <base64 diagnostics>".
//
// If the server dies or the jar can't be built, compile() returns null and
// the caller falls back to spawning kotlinc — the loop keeps working, just
// slower.

import { spawn, spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const HERE = path.dirname(new URL(import.meta.url).pathname)
export const KOTLIN_DIST = process.env.KOTLIN_DIST || path.join(HERE, '..', '..', 'dist')
const KOTLINC = path.join(KOTLIN_DIST, 'kotlinc', 'bin', 'kotlinc')
const COMPILER_JAR = path.join(KOTLIN_DIST, 'kotlinc', 'lib', 'kotlin-compiler.jar')
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
            KOTLINC, ['-cp', COMPILER_JAR, SERVER_SRC, '-d', SERVER_JAR],
            { encoding: 'utf-8' },
        )
        return r.status === 0
    }

    ensureRunning() {
        if (this.proc && this.proc.exitCode === null) return true
        if (!this.ensureJar()) return false
        this.proc = spawn(
            'java', ['-cp', `${SERVER_JAR}:${COMPILER_JAR}`, 'CompileServerKt'],
            { stdio: ['pipe', 'pipe', 'inherit'] },
        )
        // no startup banner — the server answers request-for-request
        return true
    }

    /** (ok, diagnostics) or null → the caller falls back to spawning. */
    async compile(args) {
        if (!this.ensureRunning()) return null
        try {
            this.proc.stdin.write(args.join(US) + '\n')
            const line = await this.readLine()
            const m = /^RESULT (\d+) (.*)$/.exec(line)
            if (!m) throw new Error('bad server response: ' + line.slice(0, 80))
            const diag = Buffer.from(m[2], 'base64').toString()
            return [Number(m[1]) === 0, diag]
        } catch (e) {
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
