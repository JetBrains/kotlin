package foo

var i = 0

fun test(): Int? = i++

fun box(): Boolean {
    if (i != 0) return false
    test()?.plus(1)
    if (i != 1) return false
    test()?.minus(2)
    if (i != 2) return false
    return true
}