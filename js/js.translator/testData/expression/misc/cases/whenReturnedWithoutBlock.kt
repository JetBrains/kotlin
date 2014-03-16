package foo

fun box(): Boolean {
    if (t(1) != 0) {
        return false
    }
    if (t(0) != 1) {
        return false
    }
    return (t(100) == 2)

}

fun t(i: Int) = when(i) {
    0 -> 1
    1 -> 0
    else -> 2
}