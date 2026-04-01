// ES_MODULES
// SPLIT_PER_FILE
// MODULE: lib
// FILE: lib.kt
@JsExport.Default
fun defaultExport(): String = "OK"

// FILE: test.mjs
// ENTRY_ES_MODULE
import defaultExport from "./defaultExportPerFile-kotlin_lib_v5/lib.export.mjs";

export function box() {
    return defaultExport();
}
