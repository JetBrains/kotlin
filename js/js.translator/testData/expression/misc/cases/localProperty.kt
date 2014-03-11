package foo

val y = 3

fun f(a: Int): Int {
    val x = 42
    val y = 50

    return y
}

fun box(): Int {
    return f(y)
}
