internal class A {
    private var f1 = 1.0f
    private val f2 = 1.0f
    private val f3 = 1f
    private var f4 = 1f
    private val f5 = 1f
    private val f6 = -1f
    private val f7 = -1f
    private val f8 = +1f
    private val f9 = 1f
    private val f10 = 1f

    fun foo1(f: Float) {}
    fun foo2(f: Float?) {}

    fun bar() {
        foo1(1f)
        foo2(1f)
        foo1(1f)
        foo1(1f)
        foo1(-1f)
        foo1(-1f)
        f1 = 1f
        f4 = 1.0f
    }
}