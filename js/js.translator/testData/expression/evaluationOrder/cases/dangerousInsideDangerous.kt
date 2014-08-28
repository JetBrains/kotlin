package foo

fun box(): Boolean {
    if (f(0) != 201) {
        return false
    }
    if (f(1) != 104) {
        return false
    }
    if (f(-2) != -100) {
        return false
    }
    return true
}

fun f(i: Int): Int {
    var j = i
    return ++j + if (j != 1) {
        (if (j > 0) 100 else return -100) + 2
    }
    else 200
}