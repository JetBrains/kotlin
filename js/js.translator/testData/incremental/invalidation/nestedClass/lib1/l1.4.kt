data class MyClass(val unusedField: String) {
    class NestedClass {
        inline fun foo() = 1

        val field = 5
    }

    fun unusedFunction() = -1
}
