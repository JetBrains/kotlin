// DONT_TARGET_EXACT_BACKEND: JS
// EXPECTED_REACHABLE_NODES: 1252
// ES_MODULES

// MODULE: export_all_file
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

import { B } from "./exportAllFile-export_all_file_v5.mjs";

export function box() {
    return new B().foo("K")
}