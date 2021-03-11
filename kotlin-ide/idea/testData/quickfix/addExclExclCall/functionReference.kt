// "Add non-null asserted (!!) call" "true"
class Foo {
    fun f() = 1
}

fun test(foo: Foo?) {
    val f = foo::f<caret>
}