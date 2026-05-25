fun interface A {
    fun compute(value: Int): Unit
}
fun Example(a: A) {
    a.compute(123)
}
fun Usage() {
    Example { it -> it + 1 }
}

fun used(x: Any?) {}
