open class Baz()

class Foo(val x: Int, var y: Int)

class Bar(val foo: Foo) : Baz()

class Rec(val next: Rec?)

fun <!VIPER_TEXT!>useFoo<!> (bar: Bar, rec: Rec) { }

open class A() {
    val x: Int = 1
    var y: Int = 2
}
open class B() : A()
class C() : B()

fun <!VIPER_TEXT!>three_layers_hierarchy<!>(c: C) { }

fun <!VIPER_TEXT!>list_hierarchy<!>(xs: MutableList<Int>) { }
