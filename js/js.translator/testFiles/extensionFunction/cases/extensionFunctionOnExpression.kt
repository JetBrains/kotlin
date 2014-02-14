package foo

fun Int.same(): Int {
    return this
}

fun Int.quadruple(): Int {
    return same() * 4;
}

fun box(): Boolean {
    return ((3 + 4).quadruple() == 28)
}
