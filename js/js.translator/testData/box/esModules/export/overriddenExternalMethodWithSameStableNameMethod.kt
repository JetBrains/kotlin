// EXPECTED_REACHABLE_NODES: 1252
// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES

// MODULE: lib
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
import { Baz } from "./overriddenExternalMethodWithSameStableNameMethod-lib_v5.mjs";

export function box() {
    const foo = new Baz()
    const oStable = foo.oStable("OK")
    if (oStable !== "OK") return "false: " + oStable
    return foo.o() + foo.k()
}