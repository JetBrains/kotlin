fun <!VIPER_TEXT!>shadow_local<!>(): Unit {
    var foo: Int
    val x = 0
    if (x == 0) {
        foo = x
        val x = 1
        foo = x
    } else {
        foo = x
        val x = 2
        foo = x
    }
    foo = x
}

fun <!VIPER_TEXT!>shadow_param<!>(x: Int): Unit {
    var foo: Int
    foo = x
    val x = 0
    foo = x
}

fun <!VIPER_TEXT!>shadow_nested<!>(x: Int): Unit {
    var foo: Int
    foo = x
    val x = 0
    foo = x
    if (true) {
        foo = x
        val x = 1
        foo = x
        while (true) {
            foo = x
            val x = 2
            foo = x
        }
        foo = x
    }
    foo = x
}