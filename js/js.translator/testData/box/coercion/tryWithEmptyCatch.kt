// EXPECTED_REACHABLE_NODES: 1282
fun test(x: Int): Any {
    return try {
        if (x % 2 == 0) throw RuntimeException()
        x
    }
    catch (e: RuntimeException) {
    }
}

fun box(): String {
    val a = test(1)
    if (a != 1) return "fail1: $a"

    val b = test(2)
    if (b != Unit) return "fail2: $b"

    return "OK"
}