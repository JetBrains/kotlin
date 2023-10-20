open class Baz()

class Foo(val x: Int, var y: Int)

class Bar(val foo: Foo) : Baz()

class Rec(val next: Rec?)

fun <!VIPER_TEXT!>useFoo<!> (bar: Bar, rec: Rec, l: List<Int>) {

}