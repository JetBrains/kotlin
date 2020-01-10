package simple

/**
 * Simple class.
 */
class Foo(val x: Int) {
    /**
     * Simple property.
     */
    val abc: String = "a"
        /** Simple getter. */ get

    /**
     * Simple method.
     */
    @Deprecated("foo() is deprecated")
    fun foo(): Float {
        return 0f
    }
}