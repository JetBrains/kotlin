open class Foo(val x: Int) {
    val y: Int = 3
    var b = false
    fun <!VIPER_TEXT!>getY<!>(): Int {
        return y
    }
}

open class Bar(x: Int) : Foo(x) {
    val z = 5

    fun <!VIPER_TEXT!>sum<!>(): Int = x + z
}

class Baz : Bar(1)

fun <!VIPER_TEXT!>callSuperMethod<!>(bar: Bar): Int {
    return bar.getY()
}

fun <!VIPER_TEXT!>accessSuperField<!>(bar: Bar): Boolean {
    return bar.b
}

fun <!VIPER_TEXT!>accessNewField<!>(bar: Bar): Int {
    return bar.z
}

fun <!VIPER_TEXT!>callNewMethod<!>(bar: Bar): Int {
    return bar.sum()
}

fun <!VIPER_TEXT!>setSuperField<!>(bar: Bar) {
    bar.b = true
}

fun <!VIPER_TEXT!>accessSuperSuperField<!>(baz: Baz): Int {
    return baz.x
}
