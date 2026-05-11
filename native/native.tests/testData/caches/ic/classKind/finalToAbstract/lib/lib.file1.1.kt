package test

abstract class Foo {
    abstract fun foo(): Int
}

class FooImpl : Foo() {
    override fun foo() = 42
}
