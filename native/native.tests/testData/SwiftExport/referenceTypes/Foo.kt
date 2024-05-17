class Foo(var x: Int) {
    constructor(f: Float) : this(f.toInt())

    fun getAndSetX(newX: Int): Int {
        val oldX = x
        x = newX
        return oldX
    }
}

fun getX(foo: Foo) = foo.x

fun makeFoo(x: Int) = Foo(x)

fun idFoo(foo: Foo) = foo

var globalFoo = Foo(42)

val readGlobalFoo
    get() = globalFoo

fun getGlobalFoo() = globalFoo
