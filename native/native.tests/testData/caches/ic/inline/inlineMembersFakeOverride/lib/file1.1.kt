package test

abstract class Foo {
    inline fun foo() = 10
    inline val x: Int
        get() = 20
}

class FooImpl : Foo()
