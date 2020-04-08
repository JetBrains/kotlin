interface Foo{
    fun foo1() : String
    fun foo2() : Object
    val prop : String
}

val foo = Foo()

fun f(p1: Foo, p2: String) {
    var a : String
    a = p1.<caret>
}

fun Foo.ext() : String{}

// EXIST: foo1
// ABSENT: foo2
// EXIST: prop
// EXIST: ext
// ABSENT: foo
// ABSENT: p1
// ABSENT: p2
// ABSENT: a
