// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: deprecated.kt

package foo

@JsExport
@Deprecated("message 1", level = DeprecationLevel.WARNING)
fun funktion() {}

@JsExport
@Deprecated("message 2", level = DeprecationLevel.ERROR)
val bar: String = "Test"

@JsExport
@Deprecated("message 3", level = DeprecationLevel.WARNING)
class TestClass

@JsExport
@Deprecated("hidden function", level = DeprecationLevel.HIDDEN)
fun hiddenFunction() {}

@JsExport
@Deprecated("hidden property", level = DeprecationLevel.HIDDEN)
val hiddenProperty: String = "hidden"

@JsExport
@Deprecated("hidden class", level = DeprecationLevel.HIDDEN)
class HiddenClass

@JsExport
class AnotherClass @Deprecated("message 4", level = DeprecationLevel.WARNING) constructor(val value: String) {
    @JsName("fromNothing")
    @Deprecated("message 5", level = DeprecationLevel.WARNING) constructor(): this("Test")

    @JsName("fromInt")
    constructor(value: Int): this(value.toString())

    @Deprecated("message 6", level = DeprecationLevel.ERROR)
    fun foo() {}

    fun baz() {}

    @Deprecated("deprecated read-only property", level = DeprecationLevel.WARNING)
    val readOnlyProperty: String = "Test"

    @Deprecated("deprecated read-write property", level = DeprecationLevel.ERROR)
    var readWriteProperty: String = "Test"

    @get:Deprecated("this getter is deprecated", level = DeprecationLevel.WARNING)
    var deprecatedGetter: String = "deprecatedGetter"

    @set:Deprecated("this setter is deprecated", level = DeprecationLevel.ERROR)
    var deprecatedSetter: String = "deprecatedSetter"

    @property:Deprecated("deprecated property", level = DeprecationLevel.WARNING)
    @get:Deprecated("deprecated getter", level = DeprecationLevel.ERROR)
    @set:Deprecated("deprecated setter", level = DeprecationLevel.WARNING)
    var mixedDeprecated: String = "mixedDeprecated"

    inner class Inner @Deprecated("deprecated inner class primary constructor", level = DeprecationLevel.WARNING) constructor(value: String) {
        @Deprecated("deprecated inner class secondary constructor", level = DeprecationLevel.WARNING)
        @JsName("fromNothing")
        constructor(): this("Test")
    }
}

@JsExport
interface TestInterface {
    @Deprecated("message 8", level = DeprecationLevel.WARNING)
    fun foo()
    fun bar()
    @Deprecated("message 9", level = DeprecationLevel.ERROR)
    val baz: String
}

@JsExport
object TestObject {
    @Deprecated("message 10", level = DeprecationLevel.WARNING)
    fun foo() {}
    fun bar() {}
    @Deprecated("message 11", level = DeprecationLevel.ERROR)
    val baz: String = "Test"
}

@JsExport
@Deprecated("Whole enum", level = DeprecationLevel.WARNING)
enum class TestEnum {
    @Deprecated("Only first entry", level = DeprecationLevel.ERROR)
    A,
    B,
}
