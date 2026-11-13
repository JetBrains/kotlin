// Hot-reload gate: module A built with -Xwasm-hot-reload, patch module P
// built with it too (since the stable-cell encoding, cross-mode class
// types no longer unify). P's exported funcrefs — the slot-reading
// entries — are installed into A's slots via the compiler-generated
// setters. The shared anyref exception tag makes exceptions thrown by
// code swapped into a slot catchable by Kotlin code in any generation.
//
// Gates:
//   1. function body swap (fib v1 -> v2)
//   2. METHOD body swap on an EXISTING instance (Counter.value) — vtable
//      entries reference the slot-reading entries
//   3. setter type check rejects an incompatible funcref
//   4. recovery: the slot can be swapped back

import { WASI } from 'wasi';
import fs from 'node:fs';

const sharedTag = new WebAssembly.Tag({ parameters: ['anyref'] });
const load = async (p) => {
    // one WASI instance per module (node ties WASI state to one instance)
    const wasi = new WASI({ version: 'preview1' });
    const inst = new WebAssembly.Instance(
        new WebAssembly.Module(fs.readFileSync(new URL(p, import.meta.url))),
        { ...wasi.getImportObject(), intrinsics: { tag: sharedTag } });
    // WASI reactor protocol: node's WASI refuses imports until initialized
    wasi.initialize(inst);
    return inst;
};

const A = await load('./reload_a.wasm');
const P = await load('./reload_p.wasm');

const exportByName = (inst, prefix) => {
    const names = Object.keys(inst.exports).filter((n) => n.startsWith(prefix));
    if (names.length !== 1) throw new Error(`expected exactly one ${prefix}*, got ${names}`);
    return inst.exports[names[0]];
};

// ---- gate 1: function body swap --------------------------------------
console.log('fibViaSlot(10) before swap =>', A.exports.fibViaSlot(10));   // expect 55
const setFib = exportByName(A, '__hot_set$fib#');
setFib(P.exports.fibv2);
console.log('fibViaSlot(10) after swap  =>', A.exports.fibViaSlot(10));   // expect != 55 (v2 body)

// ---- gate 2: method body swap on an EXISTING instance ----------------
const counter = A.exports.makeCounter();                                  // instance of v1 Counter
console.log('readCounter(counter) before =>', A.exports.readCounter(counter)); // 6
const setValue = exportByName(A, '__hot_set$Counter.value#');
setValue(P.exports.valuev2);
console.log('readCounter(counter) after  =>', A.exports.readCounter(counter)); // 6000 — same instance, new method body

// ---- gate 3: setter type check ---------------------------------------
try {
    setFib(P.exports.valuev2); // (ref null $Counter)->i32 into an (i32)->i32 slot
    console.log('incompatible install => UNEXPECTEDLY SUCCEEDED');
} catch (e) {
    console.log('incompatible install => rejected:', e.message.split('\n')[0]);
}

// ---- gate 4: recovery --------------------------------------------------
setFib(P.exports.fibv2);
console.log('fibCatching() after recovery     =>', A.exports.fibCatching());
