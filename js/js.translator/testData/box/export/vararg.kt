// IGNORE_BACKEND: JS
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1270
// SKIP_MINIFICATION
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: vararg
// FILE: lib.kt
@JsExport
fun uintVararg(vararg uints: UInt): String {
    for (u in uints)  {
        if (u == 0u) return "Failed"
    }

    return "OK"
}

@JsExport
fun uint(a: Int): UInt {
    return a.toUInt()
}

// FILE: test.js
function box() {
    var vararg = this.vararg
    var uintVararg = vararg.uintVararg
    var uint = vararg.uint

    return uintVararg([uint(1), uint(2), uint(3)])
}
