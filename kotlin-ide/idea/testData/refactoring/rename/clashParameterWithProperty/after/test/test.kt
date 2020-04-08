package test

class Foo {
    var foo: Int = 0
    var bar: Int = 0
}

fun makeFoo(foo: Int, _bar: Int) = Foo().apply {
    this.foo = foo
    bar = _bar
}