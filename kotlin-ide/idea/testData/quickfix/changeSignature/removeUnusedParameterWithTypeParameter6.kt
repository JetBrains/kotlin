// "Remove parameter 'x'" "true"
class Foo<X> {
    constructor(<caret>x: X)
}

val foo = Foo(1)
