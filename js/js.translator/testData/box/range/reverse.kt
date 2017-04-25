// EXPECTED_REACHABLE_NODES: 893
package foo

fun box(): String {
    assertEquals(listOf(7, 6, 5, 4, 3), (3..7).reversed().toList())
    assertEquals(listOf(10, 11, 12, 13), (13 downTo 10).reversed().toList())

    return "OK"
}