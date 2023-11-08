package interfaces

interface Foo {
    val foo: Int
    fun bar(): Double

    interface Baz: Foo {}
}

interface Bar: Foo {
    var baz: Long
}

class Impl: Foo, Bar {
    override var foo: Int = 15
    override var baz: Long = 13

    override fun bar(): Double = 42.0
}