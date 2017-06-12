// EXPECTED_REACHABLE_NODES: 502
package foo

var t: Any? = null

data class Dat(val start: String, val middle: String, val end: String) {
    override fun toString() = "another string"
    override fun hashCode() = 371
    override fun equals(other: Any?): Boolean {
        t = other
        return true
    }
}

fun box(): String {
    val d = Dat("max", "-", "min")
    val other = Dat("other", "-", "instance")

    assertEquals(371, d.hashCode())
    assertEquals(true, d == other)
    assertEquals(other, t)
    assertEquals("another string", d.toString())

    return "OK"
}

