class Foo(var x: Int)

fun <!VIPER_TEXT!>get_foo<!>(): Foo = Foo(0)
fun <!VIPER_TEXT!>side_effect<!>(): Int = 0

fun <!VIPER_TEXT!>test<!>() {
    get_foo().x = side_effect()
    val y = get_foo().x + side_effect()
}
