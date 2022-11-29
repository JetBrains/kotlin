// RUN_PLAIN_BOX_FUNCTION
// IGNORE_BACKEND: JS

// MODULE: lib
// FILE: lib.kt
@file:JsExport

val value: String = "TEST"

@JsExport.Ignore
val excludedValue: Int = 42

fun foo(): String = "FOO"

@JsExport.Ignore
fun excludedFun(): String = "EXCLUDED_FUN"

class SomeClass

@JsExport.Ignore
class ExcludedSomeClass {
    fun doSomething(): String = "SOMETHING"
}

object Companion {
    fun baz(): String = "BAZ"

    @JsExport.Ignore
    fun excludedFun(): String = "STATIC EXCLUDED_FUN"
}

// FILE: main.js
function box() {
    var lib = this.lib;

    if (lib.value !== "TEST") return "Error: exported property was not exported"
    if (lib.excludedValue === 42) return "Error: not exported property was exported"

    if (lib.foo() !== "FOO") return "Error: exported function was not exported"
    if (typeof lib.excludedFun === "function") return "Error: not exported function was exported"

    if (typeof lib.SomeClass !== "function") return "Error: exported nested class was not exported"
    if (typeof lib.ExcludedSomeClass === "function") return "Error: not exported nested class was exported"

    if (lib.Companion.baz() !== "BAZ") return "Error: exported companion function was not exported"
    if (typeof lib.Companion.excludedFun === "function") return "Error: not exported companion function was exported"

    return "OK"
}