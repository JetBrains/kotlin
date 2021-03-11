package test

class ZZZ

actual typealias Foo = ZZZ
actual class Bar

fun test() {
    val foo: Foo = Foo()
    val bar: Bar = Bar()
}