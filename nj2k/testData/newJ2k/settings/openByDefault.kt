// !OPEN_BY_DEFAULT: true
internal open class A {
    open fun foo1() {}
    private fun foo2() {}
    fun foo3() {}
}

internal class B {
    fun foo() {}
}

internal abstract class C {
    abstract fun foo()
}

internal interface I {
    fun foo()
}

internal open class D : I {
    override fun foo() {}
}

internal enum class E {
    ;

    fun foo(): Int {
        return 0
    }
}