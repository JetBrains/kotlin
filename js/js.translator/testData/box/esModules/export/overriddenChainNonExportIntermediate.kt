// DONT_TARGET_EXACT_BACKEND: JS
// EXPECTED_REACHABLE_NODES: 1252

// ES_MODULES
// MODULE: intermediate
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
import { C } from "./overriddenChainNonExportIntermediate-intermediate_v5.mjs";

export function box() {
    const c = new C()

    const foo = c.foo()
    if (foo !== "foo") return `Fail: expect c.foo() to return 'foo' but it returns ${foo}`

    const bar = c.bar()
    if (bar !== "bar") return `Fail: expect c.bar() to return 'bar' but it returns ${bar}`

    const bay = c.bay()
    if (bay !== "bay") return `Fail: expect c.bay() to return 'bay' but it returns ${bay}`

    return "OK"
}