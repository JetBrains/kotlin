package foo

fun box(): Boolean {
    val v1 = { x: Int -> x}(2)

    val f = { x: Int -> x}
    val v2 = (f)(2)

    return v1 == 2 && v2 == 2
}
