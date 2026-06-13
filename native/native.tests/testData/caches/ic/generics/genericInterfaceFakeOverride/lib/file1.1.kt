package test

interface Foo<T> {
    fun test(x: T): Int = x.hashCode()
}

open class FooImpl : Foo<Int>

class C : FooImpl()
