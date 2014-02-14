package foo

fun Int.quadruple(): Int {
    return this * 4;
}

fun box(): Boolean {
    return (3.quadruple() == 12)
}
