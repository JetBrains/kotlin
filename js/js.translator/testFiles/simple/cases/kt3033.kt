package foo

data class A(val a: Double, val b: Double)

fun box() : Boolean {
    val a = A(1.0, 1.0)
    val b = a.copy()
    if (b.a == 1.0 && b.b == 1.0) {
        return true
    }
    return false
}
