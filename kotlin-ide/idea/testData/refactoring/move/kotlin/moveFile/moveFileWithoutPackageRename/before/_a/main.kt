package a

class Test {
    val aFoo: Foo = Foo()
    val bFoo: b.Foo = b.Foo()
    val cFoo: c.Foo = c.Foo()
    val aBar: Foo.Bar = Foo.Bar()
    val bBar: b.Foo.Bar = b.Foo.Bar()
    val cBar: c.Foo.Bar = c.Foo.Bar()
}

fun test() {
    foo()
    b.foo()
    c.foo()
}

var TEST: String
    get() = FOO + b.FOO + c.FOO
    set(value: String) {
        FOO = value
        b.FOO = value
        c.FOO = value
    }