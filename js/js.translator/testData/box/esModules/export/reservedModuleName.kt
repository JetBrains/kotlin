// ES_MODULES

// MODULE: if
// FILE: lib.kt
@JsName("foo")
@JsExport
public fun foo(k: String): String = "O$k"

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { foo } from "./reservedModuleName-if_v5.mjs";

export function box() {
    return foo("K")
}