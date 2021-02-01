// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: export-all-file
// FILE: lib.kt
@file:JsExport

abstract class A {
    abstract fun foo(k: String): String
}

class B : A() {
    override fun foo(k: String): String {
        return "O" + k
    }
}

// FILE: test.js
function box() {
    return new this["export-all-file"].B().foo("K");
}