package test

abstract class Foo {
    open fun foo(): Int = 2
}

class FooImpl : Foo()
