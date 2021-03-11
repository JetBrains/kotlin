class Predicate(val x: Int) {
    operator fun invoke() = x

    operator fun unaryMinus() = Predicate(-x)
}

fun test(p: Predicate) {
    val x = -p
    println(<caret>x())
}