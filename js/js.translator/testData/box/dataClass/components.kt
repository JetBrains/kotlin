// EXPECTED_REACHABLE_NODES: 502
package foo

data class Dat(val start: String, val middle: String, val end: String) {
    fun getLabel() : String {
        return start + end
    }
}

fun box(): String {
    val d = Dat("max", "-", "min")
    assertEquals("maxmin", d.getLabel())
    val (p1, p, p2) = d
    assertEquals("max", p1)
    assertEquals("min", p2)
    return "OK"
}