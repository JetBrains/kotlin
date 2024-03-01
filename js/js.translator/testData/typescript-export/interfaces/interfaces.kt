// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: interfaces.kt

package foo

// Classes

@JsExport
interface TestInterface {
    val value: String
    fun getOwnerName(): String
}

@JsExport
interface AnotherExportedInterface

@JsExport
open class TestInterfaceImpl(override val value: String) : TestInterface {
    override fun getOwnerName() = "TestInterfaceImpl"
}

@JsExport
class ChildTestInterfaceImpl(): TestInterfaceImpl("Test"), AnotherExportedInterface

@JsExport
fun processInterface(test: TestInterface): String {
    return "Owner ${test.getOwnerName()} has value '${test.value}'"
}

@JsExport
external interface OptionalFieldsInterface {
    val required: Int
    val notRequired: Int?
}

@JsExport
interface WithTheCompanion {
    val interfaceField: String

    companion object {
        fun companionFunction(): String = "FUNCTION"
    }
}

@JsExport
fun processOptionalInterface(a: OptionalFieldsInterface): String {
    return "${a.required}${a.notRequired ?: "unknown"}"
}

// KT-63184
@JsExport
interface InterfaceWithCompanion {
    // Emulate added by plugin companion like kotlinx.serialization does
    @Suppress("WRONG_EXPORTED_DECLARATION")
    @JsExport.Ignore
    companion object {
        fun foo() = "String"
    }
}

// KT-64708
@JsExport
external interface ExportedParentInterface

@JsExport
interface ExportedChildInterface : ExportedParentInterface {
    fun bar()
}

// KT-63907
@JsExport
interface InterfaceWithDefaultArguments {
    fun foo(x: Int = 0) = x
    fun bar(x: Int = 0) = x
}

@JsExport
class ImplementorOfInterfaceWithDefaultArguments : InterfaceWithDefaultArguments {
    override fun bar(x: Int) = x + 1
}