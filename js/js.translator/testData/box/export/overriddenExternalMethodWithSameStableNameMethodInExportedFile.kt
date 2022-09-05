// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: lib
// FILE: not_exported.kt

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

// FILE: exported.kt
@file:JsExport

class Baz : Bar() {
    override fun String.o(): String {
        return this
    }
}

// FILE: test.js
function Foo() {}
Foo.prototype.k = function() {
    return "K"
}

function box() {
    return test(new this["lib"].Baz());
}

function test(foo) {
    const oStable = foo.oStable("OK")
    if (oStable !== "OK") return "false: " + oStable
    return foo.o() + foo.k()
}