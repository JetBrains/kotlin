// EXPECTED_REACHABLE_NODES: 1252
// INFER_MAIN_MODULE
// SKIP_OLD_MODULE_SYSTEMS

// MODULE: export-all-file
// FILE: lib.kt
@file:JsExport

abstract class A {
    abstract fun foo(k: String): String
}

class B : A() {
    override fun foo(k: String): String {
        return "O" + k
    }
}

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { B } from "./export-all-file/index.js";
console.assert(new B().foo("K") == "OK");