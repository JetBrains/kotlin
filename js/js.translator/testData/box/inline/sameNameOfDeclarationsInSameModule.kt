// EXPECTED_REACHABLE_NODES: 1282
fun foo(x: Int) = "int: $x"

fun foo(x: String) = "string: $x"

inline fun bar(x: Int) = foo(x)

inline fun bar(x: String) = foo(x)

fun box(): String {
    val a = bar(23)
    if (a != "int: 23") return "fail1: $a"

    val b = bar("qqq")
    if (b != "string: qqq") return "fail2: $b"

    return "OK"
}