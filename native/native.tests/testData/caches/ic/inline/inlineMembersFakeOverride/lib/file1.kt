package test

abstract class Foo {
    inline fun foo() = 1
    inline val x: Int
        get() = 2
}

class FooImpl : Foo()
