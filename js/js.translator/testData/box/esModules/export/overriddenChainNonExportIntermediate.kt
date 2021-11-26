// DONT_TARGET_EXACT_BACKEND: JS
// EXPECTED_REACHABLE_NODES: 1252
// INFER_MAIN_MODULE

// ES_MODULES
// MODULE: overriden_chain_non_export_intermediate
// FILE: lib.kt
@JsExport
abstract class A {
    abstract fun foo(): String

    abstract fun bar(): String
}

abstract class B : A() {
    abstract fun baz(): String

    override fun foo(): String = "foo"
}

@JsExport
class C : B() {
    override fun bar(): String = "bar"
    override fun baz(): String = "baz"

    fun bay(): String = "bay"
}

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { C } from "./overriden_chain_non_export_intermediate/index.js";

function test(c) {
    if (c.foo() === "foo" && c.bar() === "bar" && c.bay() == "bay") return "OK"

    return "fail"
}

console.assert(test(new C()) == "OK");