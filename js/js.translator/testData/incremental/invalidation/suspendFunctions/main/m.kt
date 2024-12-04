suspend fun suspendBox(): Int {
    val x = fooX()
    val y = fooY()
    return x + y
}

fun box() = "OK"
