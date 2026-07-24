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

@JsExport
open class Base(val name: String)

@JsExport
class Producer<out T : Base>(val value: T)

@JsExport
interface Consumer<in T : Base> {
    fun consume(value: T)
}

@JsExport
class InvariantBound<T : Base>(var value: T)

@JsExport
open class Token(val text: String)

@JsExport
class TokenBox<out T : Token>(val value: T) {
    inner class Entry {
        fun getValue(): T = value
    }
}
