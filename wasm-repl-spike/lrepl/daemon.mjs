// lrepl daemon: a persistent Node process holding the "live image".
// - instantiates the STDLIB BASE once per image (it IS the image: the
//   stdlib's hotFdefns table owns the fdefinition cells) and then every
//   generation's app DELTA against it (the ordinary cross-module
//   `__fn$`/`__vt$`/`__rt$` import channel — the multimodule port),
// - supplies @WasmImport("host", ...) funcrefs from previously loaded instances,
// - implements a minimal WASI with *capturing* stdout (expression results are
//   printed by the __eval thunk and read back by the driver),
// - speaks JSON lines over stdio with the Python driver.
//
// Only the BASE imports WASI functions — the deltas reach I/O through the
// base's stdlib — so one WASI closure bound to the BASE's memory captures
// every generation's output. LOAD IS APPLY: each delta's init resolves
// its bindings' cells by name against the base's table and installs its
// impls; the daemon holds NO binding state at all.

import fs from 'node:fs';
import readline from 'node:readline';
import { randomFillSync } from 'node:crypto';

const instances = new Map(); // gen -> WebAssembly.Instance
let base = null; // the resident stdlib instance — the image's table
let outBuf = [];

// -Xwasm-hot-reload on the wasi target imports its exception tag from
// "intrinsics"."tag" (anyref-typed). One tag object shared by every
// generation: exceptions thrown by any generation are catchable Kotlin
// exceptions in every other generation.
const sharedTag = new WebAssembly.Tag({ parameters: ['anyref'] });

function makeWasi(memRef) {
    const u32 = (p) => new DataView(memRef.mem.buffer).getUint32(p, true);
    const setU32 = (p, v) => new DataView(memRef.mem.buffer).setUint32(p, v, true);
    const capture = (buf, len) =>
        outBuf.push(Buffer.from(new Uint8Array(memRef.mem.buffer, buf, len)).toString('utf-8'));

    return {
        fd_write(fd, iovs, iovsLen, nwrittenPtr) {
            let total = 0;
            for (let i = 0; i < iovsLen; i++) {
                const buf = u32(iovs + i * 8);
                const len = u32(iovs + i * 8 + 4);
                if (fd === 1 || fd === 2) capture(buf, len);
                total += len;
            }
            setU32(nwrittenPtr, total);
            return 0; // ESUCCESS
        },
        fd_close() { return 0; },
        fd_seek() { return 70; }, // ESPIPE
        fd_fdstat_get() { return 8; }, // EBADF
        random_get(buf, len) {
            randomFillSync(new Uint8Array(memRef.mem.buffer, buf, len));
            return 0;
        },
        clock_time_get(_clock, _precision, outPtr) {
            const now = BigInt(Date.now());
            setU32(outPtr, Number(now & 0xffffffffn));
            setU32(outPtr + 4, Number((now >> 32n) & 0xffffffffn));
            return 0;
        },
        proc_exit(code) { throw new Error(`proc_exit(${code})`); },
        environ_get() { return 0; },
        environ_sizes_get(outCount, outSize) { setU32(outCount, 0); setU32(outSize, 0); return 0; },
        args_get() { return 0; },
        args_sizes_get(outCount, outSize) { setU32(outCount, 0); setU32(outSize, 0); return 0; },
        sched_yield() { return 0; },
    };
}

// instantiate a wasm module against the shared tag + a WASI closure bound
// to the given memory, with object-adoption imports answering null (the
// REPL's binding machinery IS its state sharing — every adoption is
// "no previous binding", normal cold init). extraImports carries the
// multimodule service channels (`<kotlin>` = the base's exports for a
// delta). Returns [instance, memRef].
function instantiate(bytes, hostFns, extraImports = {}) {
    const module = new WebAssembly.Module(bytes);
    const memRef = { mem: null };
    const wasi = makeWasi(memRef);
    const host = { ...hostFns };
    for (const imp of WebAssembly.Module.imports(module)) {
        if (imp.module === 'wasi_snapshot_preview1' && !wasi[imp.name]) {
            wasi[imp.name] = () => 0; // stub the ones we don't implement
        }
        if (imp.module === 'host' && imp.name.startsWith('__hot_adopt$')) {
            host[imp.name] = () => null;
        }
    }
    const instance = new WebAssembly.Instance(module, {
        wasi_snapshot_preview1: wasi,
        intrinsics: { tag: sharedTag },
        host,
        ...extraImports,
    });
    memRef.mem = instance.exports.memory || memRef.mem;
    return [instance, memRef];
}

const rl = readline.createInterface({ input: process.stdin });
rl.on('line', (line) => {
    let msg;
    try { msg = JSON.parse(line); } catch { return; }
    Promise.resolve().then(() => {
        if (msg.cmd === 'load') {
            // the stdlib base rides the FIRST load of an image (or any
            // time a dist rebuild changed it): instantiate once, run its
            // init — that registers its cells into the image's table
            if (msg.baseB64) {
                const [inst] = instantiate(Buffer.from(msg.baseB64, 'base64'), {});
                const init = inst.exports._initialize;
                if (typeof init === 'function') init();
                base = inst;
            }
            if (!base) throw new Error('daemon: no base loaded');
            // the driver's @WasmImport("host", ...) references: funcrefs
            // from previously loaded generations
            const hostFns = {};
            for (const [localName, ref] of Object.entries(msg.imports || {})) {
                const [gen, exportName] = ref;
                const inst = instances.get(gen);
                if (!inst) throw new Error(`daemon: unknown generation ${gen} for import ${localName}`);
                const fn = inst.exports[exportName];
                if (typeof fn !== 'function') throw new Error(`daemon: export ${exportName} missing in gen ${gen}`);
                hostFns[localName] = fn;
            }
            const [instance] = instantiate(Buffer.from(msg.wasmB64, 'base64'), hostFns, {
                // the multimodule channel: the delta imports the base's
                // cold code AND the image's fdefinition table through it
                '<kotlin>': base.exports,
            });
            const init = instance.exports._initialize;
            if (typeof init === 'function') {
                try {
                    init();
                } catch (e) {
                    const where = (e && e.stack) ? '\n' + String(e.stack).split('\n').slice(0, 12).join('\n') : '';
                    return { id: msg.id, ok: false, error: String((e && e.message) || e) + where };
                }
            }
            instances.set(msg.gen, instance);
            return { id: msg.id, ok: true, exports: Object.keys(instance.exports) };
        }
        if (msg.cmd === 'call') {
            outBuf = [];
            const inst = instances.get(msg.gen);
            if (!inst) throw new Error(`daemon: unknown generation ${msg.gen}`);
            const fn = inst.exports[msg.fn];
            if (typeof fn !== 'function') throw new Error(`daemon: no export '${msg.fn}' in gen ${msg.gen}`);
            try {
                const result = fn(...(msg.args || []));
                return { id: msg.id, ok: true, result, stdout: outBuf.join('') };
            } catch (e) {
                // include anything the call printed before failing (e.g. a
                // redefinition thrower explaining itself via println)
                const where = (e && e.stack) ? `\n${String(e.stack).split('\n').slice(0, 14).join('\n')}` : '';
                return { id: msg.id, ok: false, error: String((e && e.message) || e) + where, stdout: outBuf.join('') };
            }
        }
        if (msg.cmd === 'reset') {
            instances.clear();
            // the image dies WITH its base: the fdefinition table lives
            // inside the base instance, so a fresh image starts with a
            // fresh (empty) table — nothing is baked anywhere else
            base = null;
            return { id: msg.id, ok: true };
        }
        throw new Error(`daemon: unknown command ${msg.cmd}`);
    }).then(
        (resp) => process.stdout.write(JSON.stringify(resp) + '\n'),
        (e) => process.stdout.write(JSON.stringify({ id: msg.id, ok: false, error: String((e && e.message) || e) }) + '\n'),
    );
});
