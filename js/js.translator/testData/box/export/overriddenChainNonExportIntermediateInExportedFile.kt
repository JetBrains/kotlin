// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

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

// FILE: test.js

function box() {
    return test(new this["intermediate"].C());
}

function test(c) {
    if (c.foo() === "foo" && c.bar() === "bar" && c.bay() == "bay") return "OK"

    return "fail"
}