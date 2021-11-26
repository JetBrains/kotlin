// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// SKIP_DCE_DRIVEN

// MODULE: export_nested_class
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

@JsExport
object MyObject {
    class A {
        fun valueA() = "OK"
    }
    class B {
        fun valueB() = "OK"
    }
    class C {
        fun valueC() = "OK"
    }
}

// FILE: test.js
function box() {
    if (new this["export_nested_class"].B.Foo().bar("K") != "OK") return "fail 1";
    if (new this["export_nested_class"].MyObject.A().valueA() != "OK") return "fail 2";
    if (new this["export_nested_class"].MyObject.B().valueB() != "OK") return "fail 3";
    if (new this["export_nested_class"].MyObject.C().valueC() != "OK") return "fail 4";

    return "OK"
}