package test

inline val foo: Foo
    get() = Foo(10)

class Foo(val x: Int)
