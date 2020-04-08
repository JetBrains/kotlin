interface Foo {
    fun test(arg: Int)
}

open class Bar : Foo {
    override fun test(arg: Int) {}
    open fun test(a: String) {}
    open fun test2() {}
}

class Baz(val foo: Foo) : Bar(), Foo by foo {
    override <caret>fun test(a: String) = super.test(a)
}
