class Foo {
    fun foo(): Int = 42
}

fun use(foo: Foo): Int = foo.foo()
