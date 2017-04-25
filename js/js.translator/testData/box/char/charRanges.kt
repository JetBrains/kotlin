// EXPECTED_REACHABLE_NODES: 539
package foo

fun box(): String {

    assertEquals(true, 'B' in 'A'..'D')
    assertEquals(true, 'E' !in 'A'..'D')

    var s = ""
    for(char in 'A'..'D') {
        s += char
    }
    assertEquals("ABCD", s)

    return "OK"
}