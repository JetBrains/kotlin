// Field-evolution gate: additions and deletions on live instances through
// stable cells + versioned payloads — the semantics the -Xwasm-hot-reload
// Stage 1 encoding enables and Stage 2 automates. Three hand-written worlds
// mirroring the compiler's cell encoding exactly:
//
//   add1.wat  world v1: class Pt { a }
//   add2.wat  world v2: class Pt { a, b }      — b ADDED   (payload v2 sub v1)
//   del.wat   world v3: class Pt {}            — a DELETED (payload v2' sibling)
//
// Gates:
//   1. baseline old world: make/get field
//   2. identity is the cell: isV1 true for objects of every world
//   3. new code on OLD instance, ADDED field: lazy migration fires once,
//      b comes from its initform, old world keeps reading a (prefix survives)
//   4. old code on MIGRATED instance, surviving field: still works (v2 sub v1)
//   5. new code on OLD instance, surviving field: no migration needed at all
//   6. DELETION: migration reads the doomed field out, swaps payload;
//      old code then TRAPS at its version cast — the invalidation trampoline
//      (Stage 2 lowers this to a catchable exception with a decent message)

import fs from 'node:fs';
const load = async (p) => new WebAssembly.Instance(
    new WebAssembly.Module(fs.readFileSync(new URL(p, import.meta.url)))).exports;

const v1 = await load('./add1.wasm');
const v2 = await load('./add2.wasm');
const v3 = await load('./del.wasm');

const tryCall = (label, fn) => {
    try { console.log(label, '=>', fn()); } catch (e) { console.log(label, '=> TRAP:', e.message.split('\n')[0]); }
};

console.log('--- 1. old world baseline ---');
const old = v1.makeV1(11);
tryCall('v1.getA_v1(old)                    [a=11]', () => v1.getA_v1(old));

console.log('--- 2. identity is the CELL, not the payload ---');
const fresh2 = v2.makeV2(33, 55);
const fresh3 = v3.makeDel();
console.log('v1.isV1(old)=', v1.isV1(old), ' v1.isV1(fresh2)=', v1.isV1(fresh2), ' v1.isV1(fresh3)=', v1.isV1(fresh3));

console.log('--- 3. ADDED field: new code on an OLD instance (lazy migration) ---');
tryCall('v2.getB_v2(old)   [migrate fires; b=initform]', () => v2.getB_v2(old));
tryCall('v2.getB_v2(old)   [second read: already migrated]', () => v2.getB_v2(old));

console.log('--- 4. old code on the MIGRATED instance, surviving field ---');
tryCall('v1.getA_v1(old)   [payload v2 is a SUBTYPE of v1]', () => v1.getA_v1(old));

console.log('--- 5. new code on an UNmigrated instance, surviving field ---');
const old2 = v1.makeV1(77);
tryCall('v2.getA_v2(old2)  [prefix shared, no migration]', () => v2.getA_v2(old2));

console.log('--- 6. DELETED field: invalidation trampoline ---');
const old3 = v1.makeV1(99);
tryCall('v3.migrateDel(old3)  [reads doomed a out, swaps]', () => v3.migrateDel(old3));
tryCall('v3.noopDel(old3)     [new world accepts it]', () => v3.noopDel(old3));
tryCall('v1.getA_v1(old3)     [old code, deleted field]', () => v1.getA_v1(old3));
tryCall('v1.getA_v1(old)      [addition-migrated: unaffected]', () => v1.getA_v1(old));
