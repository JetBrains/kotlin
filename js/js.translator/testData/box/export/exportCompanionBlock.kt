// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// LANGUAGE: +CompanionBlocksAndExtensions

// MODULE: export_companion_block
// FILE: lib.kt

@JsExport
class MyClass {
    companion {
        val foo = "FOOOO"

        var mutable = "INITIAL"

        fun bar(): String = "BARRRR"

        val baz get() = "BAZZZZ"
    }
}

// FILE: test.js
function box() {
    var MyClass = this["export_companion_block"].MyClass;

    if (MyClass.bar() !== "BARRRR") return "FAIL: bar() problem"
    if (MyClass.foo !== "FOOOO") return "FAIL: foo problem"
    if (MyClass.baz !== "BAZZZZ") return "FAIL: baz problem"
    if (MyClass.mutable !== "INITIAL") return "FAIL: mutable before mutation"
    MyClass.mutable = "CHANGED"
    if (MyClass.mutable !== "CHANGED") return "FAIL: mutable after mutation"

    return "OK"
}
