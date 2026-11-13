import { WASI } from 'wasi';
import fs from 'node:fs';
const wasi = new WASI({ version: 'preview1' });
const load = async (p) => new WebAssembly.Instance(
    new WebAssembly.Module(fs.readFileSync(new URL(p, import.meta.url))), wasi.getImportObject());

const A2 = await load('./a2-linked.wasm');

// The importer's "host" imports are wasm funcrefs from the provider module:
const host = { makeQ: A2.exports.makeQ, readQ: A2.exports.readQ, isQ: A2.exports.isQ };
const bytes = fs.readFileSync(new URL('./importer-linked.wasm', import.meta.url));
const IMP = new WebAssembly.Instance(new WebAssembly.Module(bytes), { ...wasi.getImportObject(), host });

console.log('instantiated OK — imports type-checked against canonical GC types');
console.log('evalHostRoundTrip() =>', IMP.exports.evalHostRoundTrip());
console.log('evalVirtualDispatch() =>', IMP.exports.evalVirtualDispatch());
console.log('evalIsCheck()         =>', IMP.exports.evalIsCheck());
console.log('listener()           =>', IMP.exports.listener());
