// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// INFER_MAIN_MODULE
// SKIP_DCE_DRIVEN

// TODO: Support export of nested classes
// IGNORE_BACKEND: JS_IR

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
// ENTRY_ES_MODULE
import { B } from "./export-nested-class/index.js";
console.assert(new B.Foo().bar("K") == "OK");