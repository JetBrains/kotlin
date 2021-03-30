// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// SKIP_DCE_DRIVEN

// MODULE: export-nested-class
// FILE: lib.kt

abstract class A {
    abstract fun foo(k: String): String
}

@JsExport
class B {
    class Foo : A() {
        override fun foo(k: String): String {
            return "O" + k
        }

        fun bar(k: String): String {
            return foo(k)
        }
    }
}

// FILE: test.js
function box() {
    return new this["export-nested-class"].B.Foo().bar("K");
}