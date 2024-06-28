// RUN_PLAIN_BOX_FUNCTION
// IGNORE_BACKEND: JS

// MODULE: lib
// FILE: lib.kt
@JsExport
class Test {
    companion object {
        @JsStatic
        fun bar(): String = hidden()

        fun hidden(): String = "BARRRR"

        @JsStatic
        val foo = "FOOOO"

        @JsStatic
        val baz get() = delegated

        val delegated = "BAZZZZ"

        @JsStatic
        var mutable = "INITIAL"
    }
}

// FILE: main.js
function box() {
    var Test = this.lib.Test;

    if (Test.bar() !== "BARRRR") return "Problem with static method"
    if (typeof Test.hidden !== "undefined") return "Problem with companion methods without @JsStatic"
    if (Test.foo !== "FOOOO") return "Problem with static property"
    if (Test.baz !== "BAZZZZ") return "Problem with static property delegated to a companion property"
    if (typeof Test.delegated !== "undefined") return "Problem with companion property without @JsStatic"
    if (Test.mutable !== "INITIAL") return "Problem with mutable static property before a mutation"
    Test.mutable = "CHANGED"
    if (Test.mutable !== "CHANGED") return "Problem with mutable static property after a mutation"

    return "OK"
}