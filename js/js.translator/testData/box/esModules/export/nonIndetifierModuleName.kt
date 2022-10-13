// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_MINIFICATION
// SKIP_NODE_JS
// ES_MODULES

// MODULE: lib
// FILE: lib.kt
@JsName("foo")
@JsExport
public fun foo(k: String): String = "O$k"

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { foo } from "./nonIndetifierModuleName-lib_v5.mjs";

export function box() {
    return foo("K")
}
