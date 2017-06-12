// EXPECTED_REACHABLE_NODES: 886
package foo


fun box(): String {
    val a = ArrayList<Int>();
    return if (a.size == 0) "OK" else "fail: ${a.size}"
}