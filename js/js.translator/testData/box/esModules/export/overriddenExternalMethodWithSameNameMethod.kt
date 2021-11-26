// EXPECTED_REACHABLE_NODES: 1252
// INFER_MAIN_MODULE
// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES

// MODULE: overriden_external_method_with_same_name_method
// FILE: lib.kt
external abstract class Foo {
    abstract fun o(): String
}

abstract class Bar : Foo() {
    abstract fun String.o(): String

    override fun o(): String {
        return "O".o()
    }
}

@JsExport
class Baz : Bar() {
    override fun String.o(): String {
        return this
    }
}

// FILE: foo.js
function Foo() {}
Foo.prototype.k = function() {
    return "K"
}

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { Baz } from "./overriden_external_method_with_same_name_method/index.js";

function test(foo) {
    return foo.o() + foo.k()
}

console.assert(test(new Baz())  == "OK");