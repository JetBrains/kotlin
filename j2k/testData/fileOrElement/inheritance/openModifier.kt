import java.lang.Void

open class A {
    public open fun f1() {
    }

    public fun f2() {
    }

    private fun f3() {
    }
}

open class B : A() {
    override fun f1() {
        super.f1()
    }
}

class C : B() {
    override fun f1() {
        super.f1()
    }
}

interface I {
    public fun f()
}

class D : I {
    override fun f() {
    }
}

abstract class E {
    abstract fun f1()
    open fun f2() {
    }

    fun f3() {
    }
}

class F : E() {
    override fun f1() {
    }

    override fun f2() {
        super.f2()
    }
}
