@JsExport
class MyClass(val stepId: Int) {
    fun qux() = foo() + stepId
}

