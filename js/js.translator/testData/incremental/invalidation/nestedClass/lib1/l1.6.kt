data class MyClass(val unusedField: String) {
    class NestedClass {
        inline fun foo() = field

        val field = 6
    }

    fun unusedFunction() = -1
}
