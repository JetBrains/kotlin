// EXPECTED_REACHABLE_NODES: 990
package foo

fun box(): String {
    val i = 0x80000000 + 0x8000000
    return "OK"
}
