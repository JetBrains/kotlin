package foo

fun box(): Boolean {
    var  i = 0
    var t = ++i + if (i == 0) 0 else 2
    if (t != 3) {
        return false
    }
    return true
}
