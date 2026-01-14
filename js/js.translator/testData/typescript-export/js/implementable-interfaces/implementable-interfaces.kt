// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// LANGUAGE: +JsStaticInInterface +JsAllowExportingSuspendFunctions +JsExportInterfacesInImplementableWay
// INFER_MAIN_MODULE
// WITH_STDLIB
// MODULE: JS_TESTS
// FILE: implementable-interfaces.kt

package foo

@JsExport
interface ExportedParent {
    var parentPropertyToImplement: String
    fun anotherParentMethod(): List<String>
    // TODO: uncomment after https://jetbrains.team/p/kt/reviews/25080/timeline merge
    // suspend fun parentAsyncMethod(): String

    @get:JsName("getGetterAndSetterWithJsName")
    @set:JsName("setGetterAndSetterWithJsName")
    var getterAndSetterWithJsName: String
}

@JsExport
interface IFoo<T : Comparable<T>> : ExportedParent {
    val fooProperty: String
    fun foo(): String
    // TODO: uncomment after https://jetbrains.team/p/kt/reviews/25080/timeline merge
    // suspend fun asyncFoo(): String
    fun withDefaults(value: String = "OK"): String
    fun withBridge(x: T): T
}


@JsExport
fun callingExportedParentMethod(foo: IFoo<*>): String =
    foo.anotherParentMethod()[0]

@JsExport
fun justCallFoo(foo: IFoo<*>): String =
    foo.foo()

// TODO: uncomment after https://jetbrains.team/p/kt/reviews/25080/timeline merge
//@JsExport
//suspend fun justCallAsyncFoo(foo: IFoo<*>): String =
//    foo.asyncFoo()

// TODO: uncomment after https://jetbrains.team/p/kt/reviews/25080/timeline merge
//@JsExport
//suspend fun justCallParentAsyncMethod(foo: IFoo<*>): String =
//    foo.parentAsyncMethod()

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
fun checkIsFooInterface(foo: Any): Boolean =
    foo is IFoo<*>

@JsExport
fun checkIsExportedParentInterface(foo: Any): Boolean =
    foo is ExportedParent

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

    // TODO: uncomment after https://jetbrains.team/p/kt/reviews/25080/timeline merge
    // override suspend fun asyncFoo(): String = "OK"
    // override suspend fun parentAsyncMethod(): String = "Parent OK"
}
