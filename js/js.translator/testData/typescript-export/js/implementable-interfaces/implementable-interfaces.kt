// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// LANGUAGE: +JsStaticInInterface +JsAllowExportingSuspendFunctions +JsExportInterfacesInImplementableWay
// OPT_IN: kotlin.js.ExperimentalJsNoRuntime
// INFER_MAIN_MODULE
// WITH_STDLIB
// MODULE: JS_TESTS
// FILE: implementable-interfaces.kt

package foo

import kotlin.js.JsNoRuntime

@JsExport
fun interface FunIFace {
    fun apply(x: String): String
}

@JsExport
interface ExportedParent {
    var parentPropertyToImplement: String
    fun anotherParentMethod(): List<String>
    suspend fun parentAsyncMethod(): String

    @get:JsName("getGetterAndSetterWithJsName")
    @set:JsName("setGetterAndSetterWithJsName")
    var getterAndSetterWithJsName: String

    fun withDefaultImplementation() = "KOTLIN IMPLEMENTATION: ${anotherParentMethod()[0]}"
    fun anotherDefaultImplementation() = "FROM ExportedParent"

    var propertyWithDefaultSetter: String
        get() = "KOTLIN IMPLEMENTATION ${anotherParentMethod()[0]}"
        set(value) {}

    @get:JsName("getDefaultGetterAndSetterWithJsName")
    @set:JsName("setDefaultGetterAndSetterWithJsName")
    var defaultGetterAndSetterWithJsName: String
        get() = "KOTLIN IMPLEMENTATION ${anotherParentMethod()[0]}"
        set(value) {}
}

@JsExport
interface IFoo<T : Comparable<T>> : ExportedParent {
    val fooProperty: String
    fun foo(): String
    suspend fun asyncFoo(): String
    fun withDefaults(value: String = "OK"): String
    fun withBridge(x: T): T

    fun withDefaultsAndDefaultImplementation(value: String = "OK"): String = value
    suspend fun suspendWithDefaultImplementation() = "KOTLIN IMPLEMENTATION ${foo()}"

    fun <T> genericWithDefaultImplementation(x: T): String = "GENERIC ${x}"

    fun delegatingToSuperDefaultImplementation(): String = super.withDefaultImplementation()

    override fun anotherDefaultImplementation() = "FROM IFoo"

    val propertyWithDefaultGetter: String
        get() = "KOTLIN IMPLEMENTATION ${propertyWithDefaultSetter}"
}


@JsExport
fun makeFunInterfaceWithSam(): FunIFace = FunIFace { x -> "SAM ${x}" }

@JsExport
fun makeNoRuntimeFunInterfaceWithSam(): NoRuntimeFunIface = NoRuntimeFunIface { arrayOf("SAM from Kotlin") }

@JsExport
fun callFunInterface(f: FunIFace, x: String): String = f.apply(x)

@JsExport
fun callNoRuntimeFunInterface(f: NoRuntimeFunIface): Array<String> = f.run()

@JsExport
fun callingExportedParentMethod(foo: IFoo<*>): String =
    foo.anotherParentMethod()[0]

@JsExport
fun justCallFoo(foo: IFoo<*>): String =
    foo.foo()

@JsExport
suspend fun justCallAsyncFoo(foo: IFoo<*>): String =
    foo.asyncFoo()

@JsExport
suspend fun justCallParentAsyncMethod(foo: IFoo<*>): String =
    foo.parentAsyncMethod()

@JsExport
suspend fun justCallSuspendWithDefaultImplementation(foo: IFoo<*>): String =
    foo.suspendWithDefaultImplementation()

@JsExport
fun callingWithDefaultsWithoutParameter(foo: IFoo<*>): String =
    foo.withDefaults()

@JsExport
fun callingWithDefaultsAndDefaultImplementationWithParameter(foo: IFoo<*>): String =
    foo.withDefaultsAndDefaultImplementation("KOTLIN SIDE PARAMETER")

@JsExport
fun callingWithDefaultsAndDefaultImplementationWithoutParameter(foo: IFoo<*>): String =
    foo.withDefaultsAndDefaultImplementation()

@JsExport
fun callingWithDefaultsWithParameter(foo: IFoo<*>): String =
    foo.withDefaults("KOTLIN SIDE PARAMETER")

@JsExport
fun callingWithBridge(foo: IFoo<String>): String =
    foo.withBridge("KOTLIN SIDE")

@JsExport
fun checkIsFooInterface(foo: Any): Boolean =
    foo is IFoo<*>

@JsExport
fun checkIsExportedParentInterface(foo: Any): Boolean =
    foo is ExportedParent

@JsExport
fun callingWithDefaultImplementation(foo: IFoo<*>): String =
    foo.withDefaultImplementation()

@JsExport
fun callingAnotherWithDefaultImplementation(foo: IFoo<*>): String =
    foo.anotherDefaultImplementation()

@JsExport
fun callGenericWithDefaultImplementation(foo: IFoo<*>, x: Any?): String =
    foo.genericWithDefaultImplementation(x)

@JsExport
fun callingDelegatingToSuperDefaultImplementation(foo: IFoo<*>): String =
    foo.delegatingToSuperDefaultImplementation()

@JsExport
class KotlinFooImpl : IFoo<String> {
    override val fooProperty = "IMPLEMENTED BY KOTLIN FOO PROPERTY"
    override var parentPropertyToImplement = "IMPLEMENTED BY KOTLIN PARENT PROPERTY"

    override var getterAndSetterWithJsName: String
        get() = "KOTLIN IMPLEMENTATION ${anotherParentMethod()[0]}"
        set(value) {}

    override fun foo(): String = "OK"
    override fun anotherParentMethod() = listOf("OK")

    override fun withBridge(x: String) = "KOTLIN: $x"
    override fun withDefaults(value: String) = "KOTLIN SIDE $value"

     override suspend fun asyncFoo(): String = "OK"
     override suspend fun parentAsyncMethod(): String = "Parent OK"

     override fun delegatingToSuperDefaultImplementation(): String =
         super.delegatingToSuperDefaultImplementation()
}

@JsExport
@JsNoRuntime
interface NoRuntimeIface {
    val a: String
}

@JsExport
@JsNoRuntime
fun interface NoRuntimeFunIface {
    fun run(): Array<String>
}

@JsExport
@JsNoRuntime
interface ChildOfNoRuntime : NoRuntimeIface {
    fun child(): String
}

// Implementation of @JsNoRuntime interfaces should be possible and must not introduce any brand properties
@JsExport
class KotlinNoRuntimeImpl(override val a: String) : NoRuntimeIface

@JsExport
class KotlinChildNoRuntimeImpl(override val a: String) : ChildOfNoRuntime {
    override fun child(): String = "child-$a"
}

// "Sandwich" hierarchy: JsNoRuntime -> normal -> JsNoRuntime
@JsExport
@JsNoRuntime
interface NoRuntimeBase {
    fun base(): String
}

@JsExport
interface MidNormal : NoRuntimeBase {
    fun mid(): String
}

@JsExport
@JsNoRuntime
interface NoRuntimeLeaf : MidNormal {
    fun leaf(): String
}
