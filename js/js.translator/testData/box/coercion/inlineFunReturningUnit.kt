// EXPECTED_REACHABLE_NODES: 1280
inline fun foo(i : Int) = if (i % 2 == 0) {} else i

fun box(): String {
    val a = foo(1)
    if (a != 1) return "fail1: $a"

    val b = foo(2)
    if (b != Unit) return "fail2: $b"

    return "OK"
}