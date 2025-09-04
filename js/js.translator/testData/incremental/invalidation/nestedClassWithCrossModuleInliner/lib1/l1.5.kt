data class MyClass(val unusedField: String) {
    class NestedClass {
        inline fun foo() = field

        val field = 5
    }

    fun unusedFunction() = -1
}
