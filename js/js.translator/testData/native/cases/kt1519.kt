package foo

native
class Wow() {
    val x: Int = noImpl
    val y: Int = noImpl
}

native
fun Wow.sum(): Int = noImpl

fun Wow.dblSum(): Int {
    return 2 * sum()
}


fun box(): Boolean {
    return (Wow().dblSum() == 6)
}
