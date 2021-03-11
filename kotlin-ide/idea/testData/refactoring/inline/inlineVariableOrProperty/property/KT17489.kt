class Predicate(val x: Int) {
    operator fun plusAssign(y: Int) {}

    operator fun unaryMinus() = Predicate(-x)
}

fun test(p: Predicate) {
    val <caret>x = -p
    x += 42
}