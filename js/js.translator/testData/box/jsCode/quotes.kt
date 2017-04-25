// EXPECTED_REACHABLE_NODES: 494
package foo

fun singleQuoted(i: Int): Int = js("return i")
fun tripleQuoted(i: Int): Int = js("""return i""")
fun tripleQuotedInnerQuotes(i: Int): String = js("""return "i"""")


fun box(): String {
    assertEquals(0, singleQuoted(0))
    assertEquals(0, tripleQuoted(0))
    assertEquals("i", tripleQuotedInnerQuotes(0))

    return "OK"
}