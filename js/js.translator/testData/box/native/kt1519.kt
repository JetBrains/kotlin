package foo

@native
class Wow() {
    val x: Int = noImpl
    val y: Int = noImpl
}

@native
fun Wow.sum(): Int = noImpl

fun Wow.dblSum(): Int {
    return 2 * sum()
}


fun box(): String {
    return if (Wow().dblSum() == 6) "OK" else "fail"
}
