@JsExport
class MyClass(val stepId: Int) {
    @JsName("bar")
    fun qux() = foo() + stepId
}

