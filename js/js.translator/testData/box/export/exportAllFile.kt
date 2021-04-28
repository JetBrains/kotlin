// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// INFER_MAIN_MODULE

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