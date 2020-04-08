class Foo(val i: Int?)

fun test(foo: Foo): Int? {
    val x = foo.i!!<caret>
    return x
}