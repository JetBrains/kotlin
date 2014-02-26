package foo

fun box(): Boolean {
    var i = 0
    val c = sum(++i, if (i == 0) return false else i + 2)
    if (c != 4) {
        return false
    }
    return true
}


fun sum(a1: Int, a2: Int) = a1 + a2
