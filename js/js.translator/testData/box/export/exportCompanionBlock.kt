// RUN_PLAIN_BOX_FUNCTION
// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: lib
// FILE: lib.kt
@JsExport
class Test {
    companion {
        fun bar(value: String = "O"): String = value

        val foo = "K"
        var mutable = "INITIAL"
    }
}

// FILE: main.js
function box() {
    var Test = this.lib.Test;

    if (Test.bar() !== "O") return "FAIL: bar default"
    if (Test.bar("CHANGED") !== "CHANGED") return "FAIL: bar argument"
    if (Test.foo !== "K") return "FAIL: foo"
    if (Test.mutable !== "INITIAL") return "FAIL: mutable initial"
    Test.mutable = "CHANGED"
    if (Test.mutable !== "CHANGED") return "FAIL: mutable write"

    return "OK"
}
