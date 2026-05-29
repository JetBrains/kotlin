// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +CompanionBlocksAndExtensions
// MODULE: JS_TESTS
// FILE: companion-blocks.kt

package foo

@JsExport
class MyClass {
    companion {
        val foo = "FOOOO"

        var mutable = "INITIAL"

        fun bar(): String = "BARRRR"

        val baz get() = "BAZZZZ"
    }
}
