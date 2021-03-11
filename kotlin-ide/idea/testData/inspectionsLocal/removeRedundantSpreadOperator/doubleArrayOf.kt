fun foo(vararg x: Double) {}

fun bar() {
    foo(*<caret>doubleArrayOf(1.0))
}
