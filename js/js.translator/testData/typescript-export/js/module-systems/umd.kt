// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// JS_MODULE_KIND: UMD
// FILE: umd.kt

package foo

@JsExport
val prop = 10

@JsExport
class C(val x: Int) {
    fun doubleX() = x * 2
}

@JsExport
fun box(): String = "OK"

@JsExport.Default
fun justSomeDefaultExport() = "OK"
