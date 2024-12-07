// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// KEEP: A

// MODULE: keep_class
// FILE: lib.kt

class A {
    fun foo(): String {
        return "foo"
    }

    fun bar(): String {
        return "bar"
    }
}

@JsExport
fun bar(): A {
    return A()
}

// FILE: test.js
function box() {
    var a = this["keep_class"].bar()

    if (a.foo_26di_k$() != "foo") return "fail 1"
    if (a.bar_232r_k$() != "bar") return "fail 2"

    return "OK"
}