// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// KEEP: A

// MODULE: keep_nested_class_if_kept_top_level_class
// FILE: lib.kt

class A {
    fun foo(): String {
        return "foo"
    }

    fun bar(): String {
        return "bar"
    }

    class B {
        fun baz() = "baz"
    }
}

@JsExport
fun barA(): A {
    return A()
}

@JsExport
fun bar(): A.B {
    return A.B()
}

// FILE: test.js
function box() {
    var a = this["keep_nested_class_if_kept_top_level_class"].barA()
    var b = this["keep_nested_class_if_kept_top_level_class"].bar()

    if (a.foo_26di_k$() != "foo") return "fail 1"
    if (a.bar_232r_k$() != "bar") return "fail 2"
    if (b.baz_232z_k$() != "baz") return "fail 3"

    return "OK"
}