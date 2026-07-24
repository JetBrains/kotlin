// RUN_PLAIN_BOX_FUNCTION
// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: lib
// FILE: lib.kt
@JsExport
class Test {
    companion {
        @JsName("renamedFun")
        fun originalFun(value: String = "X"): String = value

        @JsName("renamedVal")
        val originalVal: String = "OK"

        @JsName("renamedMutable")
        var originalMutable: String = "INITIAL"
    }
}

// FILE: main.js
function box() {
    var Test = this.lib.Test;

    if (typeof Test.originalFun !== "undefined") return "FAIL: originalFun leaked"
    if (typeof Test.originalVal !== "undefined") return "FAIL: originalVal leaked"
    if (typeof Test.originalMutable !== "undefined") return "FAIL: originalMutable leaked"

    if (Test.renamedFun() !== "X") return "FAIL: renamedFun default"
    if (Test.renamedFun("Y") !== "Y") return "FAIL: renamedFun argument"
    if (Test.renamedVal !== "OK") return "FAIL: renamedVal"
    if (Test.renamedMutable !== "INITIAL") return "FAIL: renamedMutable initial"

    Test.renamedMutable = "CHANGED"
    if (Test.renamedMutable !== "CHANGED") return "FAIL: renamedMutable write"

    return "OK"
}
