class A(val n: Int) {
    operator fun unaryMinus(): A = this
}

fun test() {
    A(1).unaryMinus()
    -A(1)
}