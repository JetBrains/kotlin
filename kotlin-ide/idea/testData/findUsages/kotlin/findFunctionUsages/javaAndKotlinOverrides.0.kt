// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: overrides
open class A<T> {
    open fun <caret>foo(t: T) {
        println(t)
    }

    open fun foo(t: T, tt: T) {
        println(t)
    }
}

fun <T> A<T>.foo(t: T, x: String) {
    foo(t)
    println(x)
}

fun A<String>.foo(s: String, n: Number) {
    fun <T> A<T>.foo(t: T, x: String) {
        foo(t)
        println(x)
    }

    foo(s)
    println(n)
}

open class B: A<String>() {
    override fun foo(t: String) {
        super<A>.foo(t)
    }

    open fun baz(a: A<String>) {
        a.foo("", 0)
    }

    open fun baz(a: A<Number>) {
        a.foo(0, "")
    }
}
