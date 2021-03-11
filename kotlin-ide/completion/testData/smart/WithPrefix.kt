open class Foo
class Bar : Foo()

val xfoo = Foo()
val xbar = Bar()
val xo : Any = ""

fun f(xp1 : Foo, xp2 : Bar, xp3 : String, p4 : Foo) {
    var a : Foo
    a = x<caret>
}

// EXIST: xfoo
// EXIST: xbar
// ABSENT: xo
// EXIST: xp1
// EXIST: xp2
// ABSENT: xp3
// ABSENT: p4
