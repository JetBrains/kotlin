internal open class A {
    open fun f1() {}
    fun f2() {}
    private fun f3() {}
}

internal open class B : A() {
    override fun f1() {
        super.f1()
    }
}

internal class C : B() {
    override fun f1() {
        super.f1()
    }
}

internal interface I {
    fun f()
}

internal class D : I {
    override fun f() {}
}

internal abstract class E {
    internal abstract fun f1()
    internal open fun f2() {}
    fun f3() {}
}

internal class F : E() {
    public override fun f1() {}
    public override fun f2() {
        super.f2()
    }
}
