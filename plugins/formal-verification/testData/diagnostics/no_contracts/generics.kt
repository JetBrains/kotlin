class Box<T>(var t: T) {
    fun <!VIPER_TEXT!>genericMethod<!>(x: T): T {
        return x
    }
}

fun <!VIPER_TEXT!>createBox<!>(): Int {
    val boolBox = Box(true)
    val b = boolBox.t
    val intBox = Box(2)
    return intBox.t
}

fun <!VIPER_TEXT!>setGenericField<!>() {
    val box = Box(3)
    box.t = 5
}

fun <T> <!VIPER_TEXT!>genericFun<!>(t: T): T = t

fun <!VIPER_TEXT!>callGenericFunc<!>() {
    val x = genericFun(3)
}