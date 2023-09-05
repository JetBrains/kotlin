class Foo(val a: Int, val b: Int)

fun <!VIPER_TEXT!>createFoo<!>() {
    val f: Foo = Foo(10, 20)
    val fa = f.a
    val fb = f.b
}

class Bar(val a: Bar?)

fun <!VIPER_TEXT!>createBar<!>() {
    val b: Bar = Bar(null)
}