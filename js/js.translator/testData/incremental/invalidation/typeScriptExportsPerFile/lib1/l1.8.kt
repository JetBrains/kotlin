@JsExport
fun foo() = 0

@JsExport
@JsName("RenamedExportedClass")
class ExportedClass(val value: Int) {
    fun getValue() = value
}
