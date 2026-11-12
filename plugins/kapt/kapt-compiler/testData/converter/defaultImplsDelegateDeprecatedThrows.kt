interface I {
    @Deprecated("error", level = DeprecationLevel.ERROR)
    fun deprecatedError(): Int = 1

    @Deprecated("warning")
    fun deprecatedWarning(): Int = 2

    @Throws(IllegalArgumentException::class)
    fun throwing() {}
}

class C : I

class D(private val i: I) : I by i
