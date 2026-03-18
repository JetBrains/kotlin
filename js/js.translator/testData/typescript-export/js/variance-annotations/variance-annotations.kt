// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: variance-annotations.kt

package foo

@JsExport
class Covariant<out T>(val value: T)

@JsExport
class Contravariant<in T> {
    fun consume(value: T) {}
}

@JsExport
class Invariant<T>(var value: T)

@JsExport
class UnsafeCovariant<out T>(val value: T) {
    fun consume(value: @UnsafeVariance T) {}
}
