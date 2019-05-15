// EXPECTED_REACHABLE_NODES: 1289
package foo

class Counter {
    var count: Int = 0

    @JsName("inc")
    public fun inc() {
        count++
    }
}

fun test(c: Counter, ex: Exception): Unit = js("""
    try {
        throw ex;
    } catch (e) {
        c.inc()
    } finally {
        c.inc()
    }
""")

fun box(): String {
    val c = Counter()
    test(c, NullPointerException())
    assertEquals(2, c.count)

    return "OK"
}