// EXPECTED_REACHABLE_NODES: 990
package foo


fun box(): String {
    val t1: Any = "3"
    val t2: Any = 3
    val t3: Any = "4"
    val t4: Any = 4
    if (t3 == t4) return "fail"
    return if (t1 != t2) "OK" else "fail"
}