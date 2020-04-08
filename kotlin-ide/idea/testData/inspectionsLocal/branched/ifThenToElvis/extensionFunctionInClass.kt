class Foo {
    fun Foo.bar(): Int = 1
}

fun Foo.test(foo: Foo?): Int = <caret>if (foo == null) { 0 } else { foo.bar() }