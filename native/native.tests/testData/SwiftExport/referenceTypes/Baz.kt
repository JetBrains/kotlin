object Baz {
    var x = 0
    var foo = Foo(0)

    fun getAndSetX(newX: Int): Int {
        val oldX = x
        x = newX
        return oldX
    }

    fun getAndSetFoo(newFoo: Foo): Foo {
        val oldFoo = foo
        foo = newFoo
        return oldFoo
    }
}

fun getBaz() = Baz