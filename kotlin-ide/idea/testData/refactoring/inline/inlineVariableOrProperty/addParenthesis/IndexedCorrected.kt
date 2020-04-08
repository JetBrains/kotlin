class Predicate(val x: Int) {
    operator fun set(i: Int, j: Int) {}

    operator fun unaryMinus() = Predicate(-x)
}

fun test(p: Predicate) {
    val x = -p
    <caret>x[13] = 42
}