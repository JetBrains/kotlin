package test

interface Foo<T> {
    fun test(x: T): Int = x.hashCode()
}

open class FooImpl : Foo<Int> {
    override fun test(x: Int): Int = x + 1
}

class C : FooImpl()
