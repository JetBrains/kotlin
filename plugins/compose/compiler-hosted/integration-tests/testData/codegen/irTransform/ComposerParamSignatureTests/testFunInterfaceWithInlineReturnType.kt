inline class Color(val value: Int)
fun interface A {
    fun compute(value: Int): Color
}
fun Example(a: A) {
    Example { it -> Color(it) }
}

fun used(x: Any?) {}
