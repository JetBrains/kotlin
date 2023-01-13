// IGNORE_FIR
// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
// EXPECTED_REACHABLE_NODES: 1283

// MODULE: lib1
// FILE: lib1.kt
@JsExport
fun bar() = "O"

// MODULE: lib2(lib1)
// FILE: lib2.kt

@JsExport
fun baz() = "K"

// MODULE: main(lib2)
// FILE: main.kt

@JsExport
fun result(o: String, k: String) = o + k

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { bar, baz, result } from "./reexport_v5.mjs";

export function box() {
    const o = bar();
    const k = baz();
    return result(o, k);
}