package foo

fun f(a: Int?, b: Int.(Int) -> Int) = a?.b(2)

fun box(): Boolean {
    val c1 = f (null) {
        it + this
    } != null
    if (c1) return false;
    if (f(3) {
        it + this
    } != 5) return false
    return true;
}
