package foo

fun inc(x: Int) = x + 1

fun box(): Boolean {
    val funRef = ::inc
    return funRef(5) == 6
}