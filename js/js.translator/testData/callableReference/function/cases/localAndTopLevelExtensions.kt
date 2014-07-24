package foo

fun Int.sum0(other: Int): Int = this + other;

fun box(): String {
    fun Int.sum1(other: Int): Int = this + other

    val sum2 = {Int.(other : Int) : Int -> this + other}

    var x = 10
    x = x.sum0(5)
    x = x.sum1(5)
    x = x.sum2(5)

    var y = 10
    y = y.(Int::sum0)(5)
    y = y.(Int::sum1)(5)
    y = y.sum2(5)

    var result:String = (if (x == y && x == 25) "OK" else "x=${x} y=${y}")
    return result
}
