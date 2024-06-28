// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: regular-classes.kt

package foo

@JsExport
class A

@JsExport
class A1(val x: Int)

@JsExport
class A2(val x: String, var y: Boolean)

@JsExport
class A3 {
    val x: Int = 100
}

@JsExport
class A4<T>(val value: T) {
    fun test(): T = value
}
@JsExport
class A5 {
    companion object {
        val x = 10
    }
}

@JsExport
open class A6 {
    fun then(): Int = 42
    fun catch(): Int = 24
}

@JsExport
class GenericClassWithConstraint<T: A6>(val test: T)
