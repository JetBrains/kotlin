// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: overloadUsages
interface X<T> {
}

fun <T> X<T>.foo(a: Number, b: Number) {
    println("$a$b")
}

fun bar(x: X<String>) {
    x.foo(1, 2)
}

open class A<T>: X<T> {
    internal open fun <caret>foo(t: T) {
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

fun bar(a: A<Number>) {
    a.foo(1, "")
}

fun B.foo(s: String, n: Number) {
    fun <T> A<T>.foo(t: T, x: String) {
        foo(t)
        println(x)
    }

    foo(s)
    println(n)
}

fun bar(b: B) {
    b.foo("", 0)
}
