// EXPECTED_REACHABLE_NODES: 487
fun box(): String {
    val a = js("0xff000000")
    if (a != 4278190080.0) return "fail1: $a"

    val b = js("-0xff000000")
    if (b != -4278190080.0) return "fail2: $b"

    val c = js("10000000000")
    if (c != 10000000000.0) return "fail3: $c"

    return "OK"
}