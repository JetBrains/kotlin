// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// LANGUAGE: +JsStaticInInterface +JsAllowExportingSuspendFunctions
// INFER_MAIN_MODULE
// WITH_STDLIB
// MODULE: JS_TESTS
// FILE: interfaces.kt

package foo

@JsExport
interface ExportedParent {
    fun anotherParentMethod(): List<String>
    suspend fun parentAsyncMethod(): String
    fun withDefaultImplementation() = "KOTLIN IMPLEMENTATION: ${anotherParentMethod[0]}"
    var propertyWithDefaultSetter: String
        get() = "KOTLIN IMPLEMENTATION ${anotherParentMethod[0]}"
        set(value) {}

    @get:JsName("getGetterAndSetterWithJsName")
    @set:JsName("setGetterAndSetterWithJsName")
    var getterAndSetterWithJsName: String
        get() = "KOTLIN IMPLEMENTATION ${anotherParentMethod[0]}"
        set(value) {}
}

@JsExport
interface IFoo<T : Comparable<T>> : ExportedParent {
    fun foo(): String
    suspend fun asyncFoo(): String
    fun withDefaults(value: String = "OK"): String
    fun withBridge(x: T): T
    suspend fun suspendWithDefaultImplementation() = "KOTLIN IMPLEMENTATION ${foo()}"
    val propertyWithDefaultGetter: String
        get() = "KOTLIN IMPLEMENTATION ${propertyWithDefaultSetter}"
}


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
fun callingWithDefaultsWithoutParameter(foo: IFoo<*>): String =
    foo.withDefaults()

@JsExport
fun callingWithDefaultsWithParameter(foo: IFoo<*>): String =
    foo.withDefaults("KOTLIN SIDE PARAMETER")

@JsExport
fun callingWithBridge(foo: IFoo<String>): String =
    foo.withBridge("KOTLIN SIDE")

@JsExport
fun checkIsInterface(foo: Any): Boolean =
    foo is IFoo<*>

@JsExport
fun callingWithDefaultImplementations(foo: IFoo<*>): String =
    foo.withDefaultImplementation()

@JsExport
class KotlinFooImpl : IFoo<String> {
    override fun foo(): String = "OK"
    override fun anotherParentMethod() = listOf("OK")

    override fun withBridge(x: String) = "KOTLIN: $x"
    override fun withDefaults(value: String) = "KOTLIN SIDE $value"

    override suspend fun asyncFoo(): String = "OK"
    override suspend fun parentAsyncMethod(): String = "Parent OK"
}
