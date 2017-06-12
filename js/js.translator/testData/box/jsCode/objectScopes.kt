// EXPECTED_REACHABLE_NODES: 491
package foo

external interface Summizer {
    fun sum(a: Int, b: Int): Int
}

fun box(): String {
    val summizer1: Summizer = js("({ sum: function(a, b) { return a + b; }})")
    assertEquals(3, summizer1.sum(1, 2), "summizer1")

    val summizer2: Summizer = js("({ sum: function(a, b) { return a + b; }})")
    assertEquals(3, summizer2.sum(1, 2), "summizer2")

    return "OK";
}