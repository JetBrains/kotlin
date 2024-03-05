// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: deprecated.kt

package foo

@JsExport
@Deprecated("message 1")
fun foo() {}

@JsExport
@Deprecated("message 2")
val bar: String = "Test"

@JsExport
@Deprecated("message 3")
class TestClass

@JsExport
class AnotherClass @Deprecated("message 4") constructor(val value: String) {
    @JsName("fromNothing")
    @Deprecated("message 5") constructor(): this("Test")

    @JsName("fromInt")
    constructor(value: Int): this(value.toString())

    @Deprecated("message 6")
    fun foo() {}

    fun baz() {}

    @Deprecated("message 7")
    val bar: String = "Test"
}

@JsExport
interface TestInterface {
    @Deprecated("message 8")
    fun foo()
    fun bar()
    @Deprecated("message 9")
    val baz: String
}

@JsExport
object TestObject {
    @Deprecated("message 10")
    fun foo() {}
    fun bar() {}
    @Deprecated("message 11")
    val baz: String = "Test"
}
