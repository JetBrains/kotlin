@JsExport
class MyClass(val stepId: Int) {
    @JsName("bar")
    fun qux() = foo() + stepId
}

@JsExport
fun useExportedClass(ec: Any?): Int? {
    return 44
}
