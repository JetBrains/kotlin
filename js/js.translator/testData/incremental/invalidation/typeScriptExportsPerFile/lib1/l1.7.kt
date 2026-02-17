@JsExport
fun foo() = 0

@JsExport
class ExportedClass(val value: Int) {
    fun getValue() = value
}
