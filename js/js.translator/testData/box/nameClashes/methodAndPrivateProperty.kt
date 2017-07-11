// EXPECTED_REACHABLE_NODES: 996
package foo

fun bar() = 23

private val bar = 32

fun box(): String {
    assertEquals(23, bar())
    assertEquals(32, bar)

    return "OK"
}