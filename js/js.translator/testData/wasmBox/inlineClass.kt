inline class IC(val value: Int)

fun foo(): Int {
    val x = IC(10)
    return x.value
}

fun box(): String {
    return "OK"
}