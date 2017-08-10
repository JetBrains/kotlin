internal class A {
    private var l1 = 1L
    private var l2: Long = 1
    private val l3 = 1L
    private var l4: Long = -1
    private val l5: Long = 123456789101112
    private val l6: Long = -123456789101112
    private val l7: Long = +1
    private val l8 = +1L

    fun foo1(l: Long) {}
    fun foo2(l: Long?) {}

    fun bar() {
        foo1(1)
        foo1(1L)
        foo2(1L)
        foo1(-1)
        l1 = 10
        l2 = 10L
        l4 = 10
    }

    fun foo(z: Long) {
        val b1 = z == 1L
        val b2 = z != 1L
    }
}