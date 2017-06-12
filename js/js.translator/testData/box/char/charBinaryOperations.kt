// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {

    assertEquals('K', 'A' + 10)
    assertEquals('7', 'A' - 10)

    assertEquals(3, 'd' - 'a')

    return "OK"
}