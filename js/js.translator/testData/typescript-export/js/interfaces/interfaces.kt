// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// LANGUAGE: +JsStaticInInterface
// OPT_IN: kotlin.js.ExperimentalJsNoRuntime
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: interfaces.kt

package foo

import kotlin.js.JsNoRuntime

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

        @JsStatic
        fun companionStaticFunction(): String = "STATIC FUNCTION"
    }
}

// KT-83930
@JsExport
interface KT83930 {
    companion object {
        @JsStatic
        val hello: String = "Hello World"
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

// KT-82128
@JsExport
interface InterfaceWithNamedCompanion {
    companion object Named {
        fun companionFunction(): String = "FUNCTION"

        @JsStatic
        fun companionStaticFunction(): String = "STATIC FUNCTION"
    }
}

// KT-52800
@JsExport
sealed interface SomeSealedInterface {
    val x: String
    data class <!WRONG_EXPORTED_DECLARATION!>SomeNestedImpl(override val x: String)<!> : SomeSealedInterface
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

@JsExport
@JsNoRuntime
interface NoRuntimeSimpleInterface {
    val x: String
}

// "Sandwich" hierarchy in classic interfaces suite (no implementable-interfaces feature):
// JsNoRuntime -> normal -> JsNoRuntime
@JsExport
@JsNoRuntime
interface NRBase {
    val b: String
}

@JsExport
interface MidClassic : NRBase {
    fun mid(): Unit
}

@JsExport
@JsNoRuntime
interface NRLeaf : MidClassic {
    fun leaf(): Unit
}