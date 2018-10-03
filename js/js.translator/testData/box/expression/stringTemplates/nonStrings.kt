// EXPECTED_REACHABLE_NODES: 1282
fun box(): String {
    val x = foo()
    val y = bar()

    var r = "$x$y"
    if (r != "2342") return "fail: $r"

    r = "$y$x"
    if (r != "4223") return "fail: $r"

    r = "$x$x"
    if (r != "2323") return "fail: $r"

    r = "$y$y"
    if (r != "4242") return "fail: $r"

    return "OK"
}

fun foo(): Any = 23

fun bar() = 42