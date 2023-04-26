// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// KT-58246
// MODULE: lib
// FILE: lib.kt
@JsExport
var result = 0

@JsExport
class Test {
    init {
        result += 1
    }
}

// FILE: test.js
function box() {
    new this.lib.Test()

    if (this.lib.result != 1) return "fail (called " + result + " times): init block didn't call or called more than 1 time"

    return "OK"
}
