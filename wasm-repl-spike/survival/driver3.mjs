import { WASI } from 'wasi';
import fs from 'node:fs';
const wasi = new WASI({ version: 'preview1' });
async function load(p) {
    return new WebAssembly.Instance(
        new WebAssembly.Module(fs.readFileSync(new URL(p, import.meta.url))),
        wasi.getImportObject());
}
const A = await load('./a-linked.wasm');
const B3 = await load('./b3-linked.wasm');
const p = A.exports.makePt(4, 5);
try { console.log('B3.readPtB(p)  [growing type section] =>', B3.exports.readPtB(p)); }
catch (e) { console.log('B3.readPtB(p)  => FAILED:', e.message.split('\n')[0]); }
try { console.log('B3.sumPtB(p)   [virtual] =>', B3.exports.sumPtB(p)); }
catch (e) { console.log('B3.sumPtB(p)   => FAILED:', e.message.split('\n')[0]); }
try { console.log('B3.isPt(p)     [is-test] =>', B3.exports.isPt(p)); }
catch (e) { console.log('B3.isPt(p)     => FAILED:', e.message.split('\n')[0]); }
console.log('B3.noise()     [self-test] =>', B3.exports.noise());
