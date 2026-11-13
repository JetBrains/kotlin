// Loads a wasm-wasi module (node WASI) and runs it. Hot modules are
// REACTOR-STYLE (no start section; init exported as _initialize — the
// compiler's native hot-reload output), so init runs explicitly after
// instantiation; plain modules keep their start section (wasi.start).
//   usage: node driver_cells.mjs <module>.wasm [hot]
import { WASI } from 'wasi';
import fs from 'node:fs';
const wasmPath = process.argv[2];
const hot = process.argv[3] === 'hot';
const wasi = new WASI({ version: 'preview1' });
const imports = hot
    ? { ...wasi.getImportObject(), intrinsics: { tag: new WebAssembly.Tag({ parameters: ['anyref'] }) } }
    : wasi.getImportObject();
const inst = new WebAssembly.Instance(
    new WebAssembly.Module(fs.readFileSync(new URL(wasmPath, import.meta.url))), imports);
if (hot) {
    // reactor-style: initialize (the WASI reactor protocol — main is
    // exported under `main`, not `_start`, so both APIs are unambiguous)
    wasi.initialize(inst)
    if (typeof inst.exports.main === 'function') inst.exports.main()
} else {
    wasi.start(inst)
}
