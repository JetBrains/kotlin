// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// LANGUAGE: +JsStaticInInterface +JsAllowExportingSuspendFunctions
// INFER_MAIN_MODULE
// WITH_STDLIB
// MODULE: JS_TESTS
// FILE: interfaces.kt

package foo

interface HiddenParent {
    fun someMethod(): List<Int>
}

@JsExport
interface ExportedParent {
    fun anotherParentMethod(): List<String>
    suspend fun parentAsyncMethod(): String
}

@JsExport
interface IFoo<T : Comparable<T>> : HiddenParent, ExportedParent {
    fun foo(): String
    suspend fun asyncFoo(): String
    fun withDefaults(value: String = "OK"): String
    fun withBridge(x: T): T
    fun withDefaultImplementation() = "KOTLIN IMPLEMENTATION"
}

@JsExport
fun callingHiddenParentMethod(foo: IFoo<*>): Int =
   foo.someMethod()[0]

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
