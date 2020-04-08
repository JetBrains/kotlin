interface X {
    infix fun times(p: Int): Int
}

fun foo(x: X) {
    x.<caret>times(1)
}
