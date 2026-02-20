// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: deprecated.kt

package foo

@JsExport
@Deprecated("message 1")
fun funktion() {}

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

    @Deprecated("deprecated read-only property")
    val readOnlyProperty: String = "Test"

    @Deprecated("deprecated read-write property")
    var readWriteProperty: String = "Test"

    @get:Deprecated("this getter is deprecated")
    var deprecatedGetter: String = "deprecatedGetter"

    @set:Deprecated("this setter is deprecated")
    var deprecatedSetter: String = "deprecatedSetter"

    @property:Deprecated("deprecated property")
    @get:Deprecated("deprecated getter")
    @set:Deprecated("deprecated setter")
    var mixedDeprecated: String = "mixedDeprecated"
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

@JsExport
@Deprecated("Whole enum")
enum class TestEnum {
    @Deprecated("Only first entry")
    A,
    B,
}