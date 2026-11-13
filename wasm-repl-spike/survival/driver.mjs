// Cross-generation survival driver: module A creates a Pt, module B consumes it.
// If wasm type canonicalization unifies the two modules' rec groups, the opaque
// GC reference flows A -> JS -> B and both typed calls succeed.

import { WASI } from 'wasi';
import fs from 'node:fs';

const wasi = new WASI({ version: 'preview1' });

async function load(path) {
    const bytes = fs.readFileSync(new URL(path, import.meta.url));
    const module = new WebAssembly.Module(bytes);
    return new WebAssembly.Instance(module, wasi.getImportObject());
}

const A = await load('./a-linked.wasm');
const B = await load('./b-linked.wasm');

console.log('A exports:', Object.keys(A.exports).join(', '));
console.log('B exports:', Object.keys(B.exports).join(', '));

const p = A.exports.makePt(4, 5);
console.log('value created by A (held in JS):', p);

const checks = [
    ['A.readPtA(p)      [same-module baseline]', () => A.exports.readPtA(p)],
    ['A.sumPtA(p)       [same-module virtual] ', () => A.exports.sumPtA(p)],
    ['B.readPtB(p)      [cross-module typed]  ', () => B.exports.readPtB(p)],
    ['B.sumPtB(p)       [cross-module virtual]', () => B.exports.sumPtB(p)],
];

let failed = false;
for (const [label, fn] of checks) {
    try {
        console.log(`${label} => ${fn()}`);
    } catch (e) {
        failed = true;
        console.log(`${label} => FAILED: ${e.message}`);
    }
}
console.log(failed ? 'RESULT: survival FAILED' : 'RESULT: values SURVIVE across generations');

// ---- extended checks ----
// B's own view: type test on a foreign object, and the negative control.
const B2 = await load('./b2-linked.wasm');
try {
    console.log('B.isPt(p)         [cross-module is-test]', B.exports.isPt(p) ? 'true' : 'false');
} catch (e) {
    console.log('B.isPt(p)         => FAILED:', e.message);
}
try {
    const r = B2.exports.readPtB(p);
    console.log('B2.readPtB(p)     [DIFFERENT Pt shape] => UNEXPECTEDLY SUCCEEDED:', r);
} catch (e) {
    console.log('B2.readPtB(p)     [DIFFERENT Pt shape] => rejected:', e.message.split('\n')[0]);
}
