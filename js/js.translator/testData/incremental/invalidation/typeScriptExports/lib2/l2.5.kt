@JsExport
class MyClass(val stepId: Int) {
    @JsName("baz")
    fun qux() = foo() + stepId
}

