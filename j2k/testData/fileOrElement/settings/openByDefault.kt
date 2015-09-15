// !openByDefault: true

internal open class A {
    internal open fun foo1() {
    }

    private fun foo2() {
    }

    internal fun foo3() {
    }
}

internal class B {
    internal fun foo() {
    }
}

internal abstract class C {
    internal abstract fun foo()
}

internal interface I {
    fun foo()
}

internal open class D : I {
    override fun foo() {
    }
}

internal enum class E {
    ;
    internal fun foo(): Int {
        return 0
    }
}
