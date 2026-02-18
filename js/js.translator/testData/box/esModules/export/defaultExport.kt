// ES_MODULES
// SPLIT_PER_MODULE
// MODULE: lib
// FILE: lib.kt
@JsExport.Default
fun defaultExport(): String = "OK"

// FILE: test.mjs
// ENTRY_ES_MODULE
import defaultExport from "./defaultExport-lib_v5.mjs";

export function box() {
    return defaultExport();
}
