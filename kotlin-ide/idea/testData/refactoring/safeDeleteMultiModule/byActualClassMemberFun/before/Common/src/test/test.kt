package test

expect class Foo {
    fun foo(n: Int)
}

fun test(f: Foo) {
    f.foo(1)
}