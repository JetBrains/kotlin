// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_MINIFICATION
// INFER_MAIN_MODULE
// SKIP_NODE_JS
// ES_MODULES

// MODULE: non_identifier_module_name
// FILE: lib.kt
@JsName("foo")
@JsExport
public fun foo(k: String): String = "O$k"

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { foo } from "./non_identifier_module_name/index.js";
console.assert(foo("K") == "OK");
