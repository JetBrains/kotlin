internal class A {
    private var d1 = 1.0
    private var d2 = 1.0
    private val d3 = 1.0
    private val d4 = 1.0
    private val d5 = 1.0
    private val d6 = 1.0
    private val d7 = Math.sqrt(2.0) - 1
    private val d8 = 1.0
    private val d9 = 1.0
    private val x = 1 / (1.0 + 0)

    fun foo1(d: Double) {}
    fun foo2(d: Double?) {}

    fun bar() {
        foo1(1.0)
        foo1(1.0)
        foo1(1.0)
        foo2(1.0)
        d1 = 1.0
        d2 = 1.0
    }
}