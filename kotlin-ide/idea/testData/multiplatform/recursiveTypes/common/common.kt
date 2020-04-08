package sample

expect interface A<T : A<T>> {
    fun foo(): T
}

interface B : A<B>

fun test(a: A<*>) {
    a.foo()
    a.foo().foo()
}

fun test(b: B) {
    b.foo()
    b.foo().foo()
}
