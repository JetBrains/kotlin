// Interface-version experiment: Sq built against Shape v1, probed by Shape
// v2 code (interface gained a method). MEASURED FINDING (2026-11-11):
//   `is Shape`(v2) => TRUE   (RTTI interfaceId is FQN-hash-stable),
//   `as? Shape`    => illegal cast (structural itable type).
// `is` LIES across interface shape changes — a real correctness gap that
// whole-world compilation never exposes but module reload does. The fix
// (make interface is-checks version-aware, e.g. fold the member set into
// interfaceId) belongs to the ir2wasm slot-mode compiler work.
import { WASI } from 'wasi';
import fs from 'node:fs';
const wasi = new WASI({ version: 'preview1' });
const load = async (p) => new WebAssembly.Instance(
    new WebAssembly.Module(fs.readFileSync(new URL(p, import.meta.url))), wasi.getImportObject());
const A = await load('./ishape_a-linked.wasm');  // Shape v1: {area}
const C = await load('./ishape_c-linked.wasm');  // Shape v2: {area, name}
const sq = A.exports.makeSq(6);
console.log('v1 Sq created by A:', sq);
try { console.log('C.isShapeV2(sq) [is v2]  =>', C.exports.isShapeV2(sq)); }
catch (e) { console.log('C.isShapeV2(sq) => threw:', e.message.split('\n')[0]); }
try { console.log('C.areaOf(sq)    [v1 method via v2 code] =>', C.exports.areaOf(sq)); }
catch (e) { console.log('C.areaOf(sq)    => threw:', e.message.split('\n')[0]); }
try { console.log('C.nameOf(sq)    [v2-only method on v1 object] =>', C.exports.nameOf(sq)); }
catch (e) { console.log('C.nameOf(sq)    => threw:', e.message.split('\n')[0]); }
