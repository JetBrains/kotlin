// EXPECTED_REACHABLE_NODES: 990
package foo


fun box(): String {
    val a: Int? = 0

    val result = (a!! + 3)
    if (result != 3) return "fail: $result"
    return "OK"
}