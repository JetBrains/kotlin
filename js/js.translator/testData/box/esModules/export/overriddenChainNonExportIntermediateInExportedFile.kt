// EXPECTED_REACHABLE_NODES: 1252
// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES

// MODULE: intermediate
// FILE: not_exported.kt
abstract class B : A() {
    abstract fun baz(): String

    override fun foo(): String = "foo"
}


// FILE: exported.kt
@file:JsExport

abstract class A {
    abstract fun foo(): String

    abstract fun bar(): String
}

class C : B() {
    override fun bar(): String = "bar"
    override fun baz(): String = "baz"

    fun bay(): String = "bay"
}

// FILE: main.mjs
// ENTRY_ES_MODULE
import { C } from "./overriddenChainNonExportIntermediateInExportedFile-intermediate_v5.mjs"

export function box() {
    var c = new C();
    if (c.foo() === "foo" && c.bar() === "bar" && c.bay() == "bay") return "OK"

    return "fail"
}