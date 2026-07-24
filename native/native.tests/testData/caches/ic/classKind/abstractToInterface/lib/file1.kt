package test

abstract class Foo {
    open fun foo(): String = "Abstract class"
}

class FooImpl : Foo()
