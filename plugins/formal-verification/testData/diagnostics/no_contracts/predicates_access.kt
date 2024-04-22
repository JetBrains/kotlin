import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

open class A(val a: Int)

open class B(val b: Int) : A(0)

interface D {
    val d: Int
        get() = 2
}

class C(val x: A, var y: A) : D, B(0)

fun <!VIPER_TEXT!>accessSuperTypeProperty<!>(c: C){
    val temp = c.a
}

fun <!VIPER_TEXT!>accessNested<!>(c: C){
    val temp = c.x.a
}

fun <!VIPER_TEXT!>accessNullable<!>(x: A?){
    var n: Int
    if (x != null) {
        n = x.a
    }
}

fun <!VIPER_TEXT!>accessCast<!>(x: A){
    var n: Int
    n = (x as B).b
}

fun <!VIPER_TEXT!>accessSafeCast<!>(x: A){
    var n: Int = 0
    val y = x as? B
    if (y != null) {
        n = y.b
    }
}

fun <!VIPER_TEXT!>accessSmartCast<!>(x: A){
    var n: Int = 0
    if (x is B) {
        n = x.b
    }
}
