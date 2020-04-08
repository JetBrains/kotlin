package test

actual class /*rename*/Foo
actual class Bar

fun test() {
    val foo: Foo = Foo()
    val bar: Bar = Bar()
}