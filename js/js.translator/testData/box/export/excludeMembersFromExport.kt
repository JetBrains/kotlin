// RUN_PLAIN_BOX_FUNCTION
// IGNORE_BACKEND: JS

// MODULE: lib
// FILE: lib.kt
@JsExport
class Bar(val value: String) {
    @JsExport.Ignore
    constructor(): this("SECONDARY")

    @JsExport.Ignore
    val excludedValue: Int = 42

    fun foo(): String = "FOO"

    @JsExport.Ignore
    fun excludedFun(): String = "EXCLUDED_FUN"

    class Nested

    @JsExport.Ignore
    class ExcludedNested {
        fun doSomething(): String = "SOMETHING"
    }

    companion object {
        fun baz(): String = "BAZ"

        @JsExport.Ignore
        fun excludedFun(): String = "STATIC EXCLUDED_FUN"
    }
}

// FILE: main.js
function box() {
    var Bar = this.lib.Bar;
    var bar = new Bar("TEST");

    if (bar.value !== "TEST") return "Error: exported property was not exported"
    if (bar.excludedValue === 42) return "Error: not exported property was exported"

    if (bar.foo() !== "FOO") return "Error: exported function was not exported"
    if (typeof bar.excludedFun === "function") return "Error: not exported function was exported"

    if (typeof Bar.Nested !== "function") return "Error: exported nested class was not exported"
    if (typeof Bar.ExcludedNested === "function") return "Error: not exported nested class was exported"

    if (Bar.Companion.baz() !== "BAZ") return "Error: exported companion function was not exported"
    if (typeof Bar.Companion.excludedFun === "function") return "Error: not exported companion function was exported"

    return "OK"
}