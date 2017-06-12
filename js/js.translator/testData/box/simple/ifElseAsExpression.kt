// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    val a = 2;
    return if (a == 2) "OK" else "fail"
}