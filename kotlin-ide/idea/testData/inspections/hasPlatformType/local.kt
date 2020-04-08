// WITH_RUNTIME

fun foo() {
    val local = java.lang.String.valueOf(1)

    fun bar() = java.lang.String.valueOf(2)

    class Local {
        val local = java.lang.String.valueOf(3)

        fun bar() = java.lang.String.valueOf(4)
    }
}