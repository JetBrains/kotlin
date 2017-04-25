// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {

    assertEquals(true, 'A' < '\uFFFF')
    assertEquals(true, 'A' > '\u0000')
    assertEquals(true, 'A' <= '\u0041')
    assertEquals(true, 'A' >= '\u0041')

    return "OK"
}