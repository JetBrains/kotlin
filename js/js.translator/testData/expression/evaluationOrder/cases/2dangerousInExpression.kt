package foo

fun box(): Boolean {
    if (f(0) != -3) {
        return false
    }
    if (f(102) != 201) {
        return false;
    }
    if (f(103) != 100) {
        return false
    }
    if (f(-100) != -100) {
        return false
    }
    if (f(-99) != -201) {
        return false
    }

    return true
}

fun f(i: Int): Int {
    var j = i
    return --j + (if (j < -100) return -100 else --j) + (if (j > 100) return 100 else 0)
}
