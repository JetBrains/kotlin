package foo

native
class Wow() {
    val x: Int = js.noImpl
    val y: Int = js.noImpl
}

native
fun Wow.sum(): Int = js.noImpl

fun Wow.dblSum(): Int {
    return 2 * sum()
}


fun box(): Boolean {
    return (Wow().dblSum() == 6)
}