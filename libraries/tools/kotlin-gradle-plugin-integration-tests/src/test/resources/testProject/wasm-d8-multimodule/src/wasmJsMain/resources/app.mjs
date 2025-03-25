import * as master from './wasm-d8-multimodule-wasm-js_master.uninstantiated.mjs';
import { instantiate } from './wasm-d8-multimodule-wasm-js.uninstantiated.mjs';

const exports = (await master.instantiate({})).exports;

instantiate({"wasm-d8-multimodule-wasm-js.master" : exports})