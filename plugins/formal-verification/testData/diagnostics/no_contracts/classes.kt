// NEVER_VALIDATE

class Foo(val a: Int, val b: Int)

fun <!VIPER_TEXT!>createFoo<!>(): Foo {
    val f: Foo = Foo(10, 20)
    val fa = f.a
    val fb = f.b
    return f
}

class Bar(val a: Bar?)

fun <!VIPER_TEXT!>createFooAndBar<!>() {
    val f: Foo = Foo(10, 20)
    val b: Bar = Bar(null)
}

class Baz(val c: Int) {
    val a = 5
}

fun <!VIPER_TEXT!>createBaz<!>() {
    val b: Baz = Baz(10)
    val ba = b.a
    val bc = b.c
}