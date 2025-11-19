fun box(): String {
    val a = js("0xff000000")
    if (a != 4278190080.0) return "fail1: $a"

    val b = js("-0xff000000")
    if (b != -4278190080.0) return "fail2: $b"

    val c = js("10000000000")
    if (c != 10000000000.0) return "fail3: $c"

    val d = js("037700000000")
    if (d != 4278190080.0) return "fail4: $d"

    val e = js("-037700000000")
    if (e != -4278190080.0) return "fail5: $e"

    val f = js("0o37700000000")
    if (f != 4278190080.0) return "fail6: $f"

    val g = js("-0o37700000000")
    if (g != -4278190080.0) return "fail7: $g"

    return "OK"
}