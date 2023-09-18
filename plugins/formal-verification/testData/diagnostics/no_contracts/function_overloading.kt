class Foo
class Bar {
    fun <!VIPER_TEXT!>baz<!>(f: Foo) {  }
    fun <!VIPER_TEXT!>baz<!>(b: Bar) {  }
}

fun <!VIPER_TEXT!>fakePrint<!>(b: Bar) {  }
fun <!VIPER_TEXT!>fakePrint<!>(f: Foo) {  }
fun <!VIPER_TEXT!>fakePrint<!>(value: Int) {  }
fun <!VIPER_TEXT!>fakePrint<!>(truth: Boolean) {  }

fun <!VIPER_TEXT!>testGlobalScopeOverloading<!>() {
    fakePrint(42)
    fakePrint(true)
    fakePrint(Foo())
    fakePrint(Bar())
}

fun <!VIPER_TEXT!>testClassFunctionOverloading<!>() {
    val b = Bar()
    b.baz(Foo())
    b.baz(Bar())
}