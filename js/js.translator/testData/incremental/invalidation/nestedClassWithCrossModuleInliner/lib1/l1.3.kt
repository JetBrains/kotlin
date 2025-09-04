data class MyClass(val unusedField: String) {
    class NestedClass {
        inline fun foo() = 1
    }

    fun unusedFunction() = -1
}
