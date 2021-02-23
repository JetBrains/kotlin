// EXPECTED_REACHABLE_NODES: 1270
// SKIP_MINIFICATION
// INFER_MAIN_MODULE
// SKIP_NODE_JS

// MODULE: non-identifier-module-name
// FILE: lib.kt
@JsName("foo")
@JsExport
public fun foo(k: String): String = "O$k"

// FILE: test.mjs
// ENTRY_ES_MODULE
import { foo } from "./non-identifier-module-name/index.js";
console.assert(foo("K") == "OK");
