fun <!VIPER_TEXT!>createFoo<!>() {
    val f: Foo = Foo(10, 20)
    val fa = f.a
    val fb = f.b
}

class Foo(val a: Int, val b: Int)
