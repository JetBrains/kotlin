// EXPECTED_REACHABLE_NODES: 1374
package foo


fun box(): String {
    val al = ArrayList<Int>(10)
    return if (al.size == 0) "OK" else "fail: ${al.size}"
}