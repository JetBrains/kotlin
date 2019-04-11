internal abstract class C {
    fun foo() {
        val s1 = f()!!

        val s2 = g() ?: error("g should not return null")
        val h = s2.hashCode()
    }

    internal abstract fun f(): String?
    internal abstract fun g(): String?
}