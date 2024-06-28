// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// MODULE_KIND: ES
// FILE: esm.kt

package foo

@JsExport
val value = 10

@JsExport
var variable = 10

@JsExport
class C(val x: Int) {
    fun doubleX() = x * 2
}

@JsExport
object O {
    val value = 10
}

@JsExport
object Parent {
    val value = 10
    class Nested {
        val value = 10
    }
}

@JsExport
fun box(): String = "OK"