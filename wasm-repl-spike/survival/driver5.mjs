// Type-version experiment: Pt v1 instance (a,b) meets Pt v2 code (a,b,c).
// Expect: typed v2 boundary rejects the v1 object; `is Pt`(v2) is FALSE
// (structural); the object is still a valid Any. Regression anchor for
// class-shape-change semantics.
import { WASI } from 'wasi';
import fs from 'node:fs';
const wasi = new WASI({ version: 'preview1' });
const load = async (p) => new WebAssembly.Instance(
    new WebAssembly.Module(fs.readFileSync(new URL(p, import.meta.url))), wasi.getImportObject());
const A = await load('./a-linked.wasm');   // Pt v1: (a, b)
const C = await load('./c-linked.wasm');   // Pt v2: (a, b, c)
const p1 = A.exports.makePt(4, 5);
console.log('v1 instance created by A:', p1);
try { console.log('C.readPtV2(p1)  [typed v2 boundary] =>', C.exports.readPtV2(p1)); }
catch (e) { console.log('C.readPtV2(p1)  [typed v2 boundary] => rejected:', e.message.split('\n')[0]); }
try { console.log('C.isPtV2(p1)   [is v2] =>', C.exports.isPtV2(p1)); }
catch (e) { console.log('C.isPtV2(p1)   [is v2] => threw:', e.message.split('\n')[0]); }
try { console.log('C.isAnySafe(p1)[is Any] =>', C.exports.isAnySafe(p1)); }
catch (e) { console.log('C.isAnySafe(p1)[is Any] => threw:', e.message.split('\n')[0]); }
