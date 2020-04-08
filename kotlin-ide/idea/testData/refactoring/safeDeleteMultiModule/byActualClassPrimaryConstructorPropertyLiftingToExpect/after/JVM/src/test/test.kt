package test

actual class Foo(s: String) {
    actual constructor(): this("") {
        val x = n + 1
    }
}

fun test() {
    Foo("1")
    Foo(s = "1")
    Foo()
    Foo()
}