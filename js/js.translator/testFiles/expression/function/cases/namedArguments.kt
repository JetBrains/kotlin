package foo

fun test(x: Int, y: Int) = y - x

fun box(): Boolean {
    if (test(1, 2) != 1) {
        return false;
    }
    if (test(x = 1, y = 2) != 1) {
        return false;
    }
    if (test(y = 2, x = 1) != 1) {
        return false;
    }
    return true
}