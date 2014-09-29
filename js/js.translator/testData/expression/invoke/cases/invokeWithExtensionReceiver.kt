package foo

fun Int.invoke(x: Int) = this + x
fun box(): Boolean {
    return 1(2) == 3
}
