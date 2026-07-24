package test

inline val foo: Foo
    get() = Foo(5)

class Foo(val x: Int)
