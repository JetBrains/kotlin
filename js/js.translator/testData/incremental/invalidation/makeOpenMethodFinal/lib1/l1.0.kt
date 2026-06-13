abstract class Foo {
    abstract fun foo(): Int
}

class FooImpl : Foo() {
    override fun foo(): Int = 42
}

fun use(foo: Foo): Int = foo.foo()
