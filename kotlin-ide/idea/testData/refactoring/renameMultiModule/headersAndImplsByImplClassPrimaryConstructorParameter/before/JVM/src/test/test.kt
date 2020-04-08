package test

actual class Foo(s: String) {
    actual constructor(n: Int): this("")
}

fun test() {
    Foo("1")
    Foo(s = "1")
    Foo(1)
    Foo(n = 1)
}