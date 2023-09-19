// Check class generation.
class Foo(val x: Int)

fun <!VIPER_TEXT!>f<!>() {
    val foo = Foo(0)
}