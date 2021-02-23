// IGNORE_BACKEND: JS
// EXPECTED_REACHABLE_NODES: 1270
// SKIP_MINIFICATION
// INFER_MAIN_MODULE
// ES_MODULES

// MODULE: if
// FILE: lib.kt
@JsName("foo")
@JsExport
public fun foo(k: String): String = "O$k"

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { foo } from "./if/index.js";

console.assert(foo("K") == "OK");
