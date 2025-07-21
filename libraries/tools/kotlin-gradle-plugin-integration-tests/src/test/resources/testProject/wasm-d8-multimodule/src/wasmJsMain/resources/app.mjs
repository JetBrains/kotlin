import * as master from './wasm-d8-multimodule_master.uninstantiated.mjs';
import { instantiate } from './wasm-d8-multimodule.uninstantiated.mjs';

const exports = (await master.instantiate({})).exports;

instantiate({"wasm-d8-multimodule.master" : exports})