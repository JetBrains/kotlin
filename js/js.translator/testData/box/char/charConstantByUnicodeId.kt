// EXPECTED_REACHABLE_NODES: 942
package foo

fun box(): String {
    val chars = mapOf('\u0000' to 0x0000,
                      '\u0001' to 0x0001,
                      '\u0010' to 0x0010,
                      '\u001F' to 0x001F,
                      '\u0064' to 0x0064,
                      '\u00A0' to 0x00A0,
                      '\u00FF' to 0x00FF,
                      '\u0100' to 0x0100,
                      '\uF01F' to 0xF01F)

    for ((char, code) in chars) {
        assertEquals(code, char.toInt())
        assertEquals(char, code.toChar())
    }

    return "OK"
}
