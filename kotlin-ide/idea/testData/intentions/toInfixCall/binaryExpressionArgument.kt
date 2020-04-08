interface X {
    infix fun compareTo(p: Int): Int
}

fun foo(x: X) {
    x.<caret>compareTo(1 + 2)
}
