class Foo(val x: Int)

fun getX(foo: Foo) = foo.x

fun makeFoo(x: Int) = Foo(x)

fun idFoo(foo: Foo) = foo

var globalFoo = Foo(42)

val readGlobalFoo
    get() = globalFoo

fun getGlobalFoo() = globalFoo