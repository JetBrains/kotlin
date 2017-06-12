// EXPECTED_REACHABLE_NODES: 497
data class A(val number: Int)

external fun foo(first: A, second: A): Boolean

external class B(value: Int)

fun box(): String {
    val a = A(23)
    val b = A(23)
    val c = A(42)

    if (!foo(a, b)) return "fail1"
    if (!foo(a, a)) return "fail2"
    if (foo(a, c)) return "fail3"

    val d = B(23)
    val e = B(23)
    val f = B(42)

    if (d != e) return "fail4"
    if (d != d) return "fail5"
    if (d == f) return "fail6"

    return "OK"
}