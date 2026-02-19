abstract class A {
    abstract fun a0()
    abstract fun a1(i: Int)
    abstract fun a2(b: Boolean, a: Any)
}

class B: A() {
    override fun a0() {}
    override fun a1(i: Int) {}
    override fun a2(b: Boolean, a: Any) {}

    fun b0() {}
}