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
interface InterfaceWithJsStaticVar {
    companion object {
        @JsStatic
        var mutable: String = "INITIAL"
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
    data class SomeNestedImpl(override val x: String) : SomeSealedInterface
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

// KT-84710
@JsExport
public interface WithDefaultSuspend {
    public fun regularWithDefault(): String = "OK"
    public suspend fun suspendWithDefault(): String = "OK"
}

@JsExport
public class WithDefaultSuspendImpl : WithDefaultSuspend

@JsExport
suspend fun callComposedDefaultSuspend(value: AbstractAndDefaultSuspend): String =
    value.defaultSuspend()

@JsExport
interface AbstractAndDefaultSuspend {
    suspend fun abstractSuspend(): String
    suspend fun defaultSuspend(): String = "${abstractSuspend()} DEFAULT"
}

@JsExport
class AbstractAndDefaultSuspendImpl : AbstractAndDefaultSuspend {
    override suspend fun abstractSuspend(): String = "ABSTRACT"
}

@JsExport
suspend fun callOuterDefaultSuspend(value: ChainedDefaultSuspend): String =
    value.outerSuspendDefault()

@JsExport
interface ChainedDefaultSuspend {
    suspend fun innerSuspendDefault(): String = "INNER"
    suspend fun outerSuspendDefault(): String = "${innerSuspendDefault()} OUTER"
}

@JsExport
class ChainedDefaultSuspendImpl : ChainedDefaultSuspend

@JsExport
suspend fun callDiamondDefaultSuspend(value: BaseDiamondDefaultSuspend): String =
    value.suspendDefault()

@JsExport
interface BaseDiamondDefaultSuspend {
    suspend fun suspendDefault(): String = "DIAMOND"
}

@JsExport
interface LeftDiamondDefaultSuspend : BaseDiamondDefaultSuspend

@JsExport
interface RightDiamondDefaultSuspend : BaseDiamondDefaultSuspend

@JsExport
class DiamondDefaultSuspendImpl : LeftDiamondDefaultSuspend, RightDiamondDefaultSuspend

@JsExport
suspend fun callGenericDefaultSuspend(value: GenericDefaultSuspend<String>, input: String): String =
    value.echoSuspendDefault(input)

@JsExport
interface GenericDefaultSuspend<T> {
    suspend fun echoSuspendDefault(input: T): T = input
}

@JsExport
class StringGenericDefaultSuspendImpl : GenericDefaultSuspend<String>

@JsExport
suspend fun callChainDefaultSuspend(value: ChainDefaultSuspend, input: String): String =
    value.suspendDefault(input)

@JsExport
interface ChainDefaultSuspend {
    suspend fun suspendDefault(input: String = "OK"): String = "CHAIN $input"
}

@JsExport
open class MidChainDefaultSuspendImpl : ChainDefaultSuspend

@JsExport
class LeafChainDefaultSuspendImpl : MidChainDefaultSuspendImpl()

@JsExport
suspend fun callLeftDefaultSuspend(value: LeftDefaultSuspend): String =
    value.leftSuspendDefault()

@JsExport
suspend fun callRightDefaultSuspend(value: RightDefaultSuspend): String =
    value.rightSuspendDefault()

@JsExport
interface LeftDefaultSuspend {
    suspend fun leftSuspendDefault(): String = "LEFT"
}

@JsExport
interface RightDefaultSuspend {
    suspend fun rightSuspendDefault(): String = "RIGHT"
}

@JsExport
class MultipleInterfaceDefaultsImpl : LeftDefaultSuspend, RightDefaultSuspend

@JsExport
suspend fun callNullableDefaultSuspend(value: NullableDefaultSuspend): String? =
    value.suspendDefault()

@JsExport
interface NullableDefaultSuspend {
    suspend fun suspendDefault(): String? = null
}

@JsExport
class NullableDefaultSuspendImpl : NullableDefaultSuspend

@JsExport
suspend fun callParameterizedDefaultSuspend(value: ParameterizedDefaultSuspend, input: String): String =
    value.suspendDefault(input)

@JsExport
interface ParameterizedDefaultSuspend {
    suspend fun suspendDefault(input: String = "OK"): String = "VALUE $input"
}

@JsExport
class ParameterizedDefaultSuspendImpl : ParameterizedDefaultSuspend

@JsExport
suspend fun callUnitDefaultSuspend(value: UnitDefaultSuspend): Unit =
    value.runDefault()

@JsExport
interface UnitDefaultSuspend {
    suspend fun runDefault(): Unit = Unit
}

@JsExport
class UnitDefaultSuspendImpl : UnitDefaultSuspend

@JsExport
suspend fun callParentSuspend(holder: HolderOfInheritedSuspend, value: String): String =
    holder.parentSuspend(value)

@JsExport
interface HolderOfInheritedSuspend {
    suspend fun parentSuspend(value: String): String
}

@JsExport.Ignore
open class HiddenSuspendParent : HolderOfInheritedSuspend {
    override suspend fun parentSuspend(value: String): String = "PARENT $value"
}

@JsExport
class ExportedSuspendChild : HiddenSuspendParent() {
    suspend fun childSuspend(): String = "CHILD"
}

@JsExport
suspend fun callOverrideSuspend(value: OverridableSuspend): String =
    value.suspendDefault()

@JsExport
interface OverridableSuspend {
    suspend fun suspendDefault(): String = "DEFAULT"
}

@JsExport
class InheritingSuspendImpl : OverridableSuspend

@JsExport
class OverridingSuspendImpl : OverridableSuspend {
    override suspend fun suspendDefault(): String = "OVERRIDDEN"
}

// KT-85038
@JsExport
sealed external interface ExternalInterfaceWithCompanion {
    @JsExport.Ignore
    companion object {
        @JsStatic val x: String
    }
}

// KT-85038
@JsExport
sealed external interface ExternalInterfaceWithSelfTypedCompanion {
    @JsExport.Ignore
    companion object {
        @JsStatic val left: ExternalInterfaceWithSelfTypedCompanion
        @JsStatic val right: ExternalInterfaceWithSelfTypedCompanion
    }
}

@JsExport
external interface ExternalInterfaceWithIgnoredNonStaticCompanion {
    @JsExport.Ignore
    companion object {
        fun hidden(): String
    }
}
