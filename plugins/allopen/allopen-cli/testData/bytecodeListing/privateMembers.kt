annotation class AllOpen

@AllOpen
private class Test {
    fun publicMethod() {}
    val publicProp: String = ""

    protected fun protectedMethod() {}
    protected val protectedProp: String = ""

    private fun privateMethod() {}
    private val privateProp: String = ""

    internal fun internalMethod() {}
    internal val internalProp: String = ""

    private tailrec fun privateTailrecMethod() {}
}