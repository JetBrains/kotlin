// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: data-classes.kt

package foo


@JsExport
data class TestDataClass(val name: String) {
    class Nested {
        val prop: String = "hello"
    }
}

@JsExport
data class KT39423(
    val a: String,
    val b: Int? = null
)