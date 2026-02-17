@JsExport
fun foo() = 0

@JsExport.Default
class ExportedClass(val value: Int) {
    fun getValue() = value
}