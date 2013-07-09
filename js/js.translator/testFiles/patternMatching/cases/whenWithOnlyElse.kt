package foo

fun box(): Boolean {
    var a = 0
    when (a) {
        else -> a = 2
    }
    return a == 2
}
