package foo

fun box(): Boolean {
    var a = 0
    var i = 0
    when(i++) {
        -100 -> a++
        100 -> a++
        else -> a++
    }
    return (a == 1) && (i == 1)
}