// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// WITH_STDLIB
// FILE: declarations.kt

package foo

@JsExport
interface ExportedInterface {
    @JsExport.Ignore
    val baz: String

    @JsExport.Ignore
    fun inter(): String

    @JsExport.Ignore
    class NotExportableNestedInsideInterface

    @JsExport.Ignore
    companion object {
        val foo: String ="FOO"
    }
}

@JsExport
class OnlyFooParamExported(val foo: String) : ExportedInterface {
    @JsExport.Ignore
    constructor() : this("TEST")

    override val baz = "Baz"

    override fun inter(): String = "Inter"

    @JsExport.Ignore
    val bar = "Bar"

    @JsExport.Ignore
    inline fun <A, reified B> A.notExportableReified(): Boolean = this is B

    @JsExport.Ignore
    suspend fun notExportableSuspend(): String = "SuspendResult"

    @JsExport.Ignore
    fun notExportableReturn(): List<String> = listOf("1", "2")

    @JsExport.Ignore
    val String.notExportableExentsionProperty: String
        get() = "notExportableExentsionProperty"

    @JsExport.Ignore
    annotation class NotExportableAnnotation

    @JsExport.Ignore
    value class NotExportableInlineClass(val value: Int)
}