// lrepl daemon: a persistent Node process holding the "live image".
// - instantiates generation modules,
// - supplies @WasmImport("host", ...) funcrefs from previously loaded instances,
// - implements a minimal WASI with *capturing* stdout (expression results are
//   printed by the __eval thunk and read back by the driver),
// - speaks JSON lines over stdio with the Python driver.
//
// Each instance gets its OWN WASI closure bound to that instance's exported
// memory: lazy val initializers mean one generation's WASI calls can be
// triggered from inside another generation's code (e.g. gen2's thunk calls
// gen1's accessor, which lazily initializes and calls random_get), so WASI
// functions must always operate on the memory of the module that invoked
// them — a single shared memory reference would corrupt the wrong image.

import fs from 'node:fs';
import readline from 'node:readline';
import { randomFillSync } from 'node:crypto';

const instances = new Map(); // gen -> WebAssembly.Instance
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

function buildImportObject(wasi, imports, adopts = []) {
    const host = {};
    for (const [localName, ref] of Object.entries(imports)) {
        const [gen, exportName] = ref;
        const inst = instances.get(gen);
        if (!inst) throw new Error(`daemon: unknown generation ${gen} for import ${localName}`);
        const fn = inst.exports[exportName];
        if (typeof fn !== 'function') throw new Error(`daemon: export ${exportName} missing in gen ${gen}`);
        host[localName] = fn;
    }
    // object-adoption imports answer null here: the REPL's binding
    // machinery IS its state sharing (objects re-declare per generation;
    // the driver routes references through accessors), so every adoption
    // is "no previous binding" — normal cold init, REPL semantics unchanged
    for (const name of adopts) host[name] = () => null;
    return { wasi_snapshot_preview1: wasi, intrinsics: { tag: sharedTag }, host };
}

const rl = readline.createInterface({ input: process.stdin });
rl.on('line', (line) => {
    let msg;
    try { msg = JSON.parse(line); } catch { return; }
    Promise.resolve().then(() => {
        if (msg.cmd === 'load') {
            // the image travels as bytes; the path is a legacy fallback
            const bytes = msg.wasmB64
                ? Buffer.from(msg.wasmB64, 'base64')
                : fs.readFileSync(msg.wasmPath);
            const module = new WebAssembly.Module(bytes);
            const memRef = { mem: null };
            const wasi = makeWasi(memRef);
            // Stub out any WASI function we don't implement (with errno 0).
            const adopts = [];
            for (const imp of WebAssembly.Module.imports(module)) {
                if (imp.module === 'wasi_snapshot_preview1' && !wasi[imp.name]) {
                    wasi[imp.name] = () => 0;
                }
                if (imp.module === 'host' && imp.name.startsWith('__hot_adopt$')) {
                    adopts.push(imp.name);
                }
            }
            const instance = new WebAssembly.Instance(module, buildImportObject(wasi, msg.imports || {}, adopts));
            memRef.mem = instance.exports.memory;
            // val initializers run AFTER wiring this instance's memory into
            // its WASI closure. New modules are REACTOR-STYLE (hot mode: the
            // compiler exports init as _initialize and emits no start
            // section); __lrepl_init is the legacy binary-patched name.
            const init = typeof instance.exports._initialize === 'function'
                ? instance.exports._initialize
                : instance.exports.__lrepl_init
            if (typeof init === 'function') {
                init()
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
                return { id: msg.id, ok: false, error: String((e && e.message) || e), stdout: outBuf.join('') };
            }
        }
        if (msg.cmd === 'swapDirect') {
            // Compiler-slot redefinition (-Xwasm-hot-reload): the new
            // implementation IS the donor's exported entry funcref — install
            // it directly into the slot owner's compiler-generated setter.
            const donor = instances.get(msg.newGen);
            const owner = instances.get(msg.slotGen);
            if (!donor) throw new Error(`daemon: unknown donor generation ${msg.newGen}`);
            if (!owner) throw new Error(`daemon: unknown slot generation ${msg.slotGen}`);
            const impl = donor.exports[msg.newExport];
            if (typeof impl !== 'function') throw new Error(`daemon: ${msg.newExport} is not a funcref`);
            owner.exports[msg.slotExport](impl);
            return { id: msg.id, ok: true };
        }
        if (msg.cmd === 'reset') {
            instances.clear();
            return { id: msg.id, ok: true };
        }
        throw new Error(`daemon: unknown command ${msg.cmd}`);
    }).then(
        (resp) => process.stdout.write(JSON.stringify(resp) + '\n'),
        (e) => process.stdout.write(JSON.stringify({ id: msg.id, ok: false, error: String((e && e.message) || e) }) + '\n'),
    );
});
