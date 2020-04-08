// WITH_RUNTIME
class Foo(var bar: Bar)
class Bar(var baz: Int)

fun test() {
    val foo = Foo(Bar(1))
    println(<selection>foo.bar</selection>)
    foo.bar.baz = foo.bar.baz + 1
}