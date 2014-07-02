package foo

fun inc(x: Int) = x + 1

fun tmp():Function1<Int, Int> {
    return ::inc
}

fun box(): Boolean {
    return tmp()(5) == 6
}