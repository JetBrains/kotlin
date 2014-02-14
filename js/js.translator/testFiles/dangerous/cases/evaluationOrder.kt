package foo

var d = 0

fun f(): Int {
    d = if (d < 0) -100 else 100
    return d
}

fun box(): Boolean {
    d = d-- + f() + when(d) {
        -100 -> return true
        1 -> 1
        else -> return false
    }
    return false
}
