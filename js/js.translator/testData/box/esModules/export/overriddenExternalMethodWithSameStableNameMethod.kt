// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// INFER_MAIN_MODULE
// ES_MODULES

// MODULE: overriden-external-method-with-same-stable-name-method
// FILE: lib.kt
external abstract class Foo {
    abstract fun o(): String
}

abstract class Bar : Foo() {
    @JsName("oStable")
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
import { Baz } from "./overriden-external-method-with-same-stable-name-method/index.js";

function test(foo) {
    const oStable = foo.oStable("OK")
    if (oStable !== "OK") return "false: " + oStable
    return foo.o() + foo.k()
}

console.assert(test(new Baz()) == "OK");