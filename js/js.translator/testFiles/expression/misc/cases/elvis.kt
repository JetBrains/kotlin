package foo

fun box(): Boolean {
    if (f(null) != false) {
        return false;
    }
    if (f(2) != true) {
        return false;
    }
    if (f(1) != false) {
        return true;
    }
    return true
}

fun Int.isEven() = (this % 2) == 0

fun f(a: Int?): Boolean {
    return a?.isEven() ?: false

}