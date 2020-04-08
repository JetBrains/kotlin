interface Foo{
    public fun foo() : Foo
    public fun s() : String
    public val prop : String
}

val foo = Foo()

fun f(p1: Foo, p2: String) {
    var a : String
    a = p1.foo().<caret>
}

// ABSENT: foo
// EXIST: s
// EXIST: prop
// ABSENT: foo
// ABSENT: p1
// ABSENT: p2
// ABSENT: a
