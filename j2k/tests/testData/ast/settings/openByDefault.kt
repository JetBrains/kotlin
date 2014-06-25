// !openByDefault: true

open class A {
    open fun foo1() {
    }
    private fun foo2() {
    }
    fun foo3() {
    }
}

class B {
    fun foo() {
    }
}

abstract class C {
    abstract fun foo()
}

trait I {
    public fun foo()
}

open class D : I {
    override fun foo() {
    }
}

enum class E {
    fun foo(): Int {
        return 0
    }
}