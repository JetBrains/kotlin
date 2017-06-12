// EXPECTED_REACHABLE_NODES: 492
package foo

external interface Summizer {
    fun sum(a: Int, b: Int): Int
}

fun getSummizer(): Summizer = js("""
    var summizer = {
        sum: function(a, b) { return a + b;}
    };

    return summizer;
""");

fun box(): String {
    val summizer = getSummizer()
    assertEquals(3, summizer.sum(1, 2))

    return "OK"
}