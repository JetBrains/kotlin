package test

abstract class Foo {
    abstract fun foo(): Int
}

class FooImpl1 : Foo() {
    override fun foo() = 42
}

class FooImpl2 : Foo() {
    override fun foo() = 117
}
