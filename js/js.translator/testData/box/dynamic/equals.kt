// EXPECTED_REACHABLE_NODES: 536
package foo

object f {
    var equalsCalled = 0

    override fun equals(other: Any?): Boolean {
        equalsCalled++
        if (equalsCalled > 1) return false
        return super.equals(other)
    }
}

fun box(): String {
    val a: dynamic = 12
    var b: dynamic = 33.4
    var c: dynamic = "text"
    val d: dynamic = true

    val v: dynamic = 42
    val tt: dynamic = "object t {}"

    testFalse { a == 34 }
    testFalse { a == "34" }
    testTrue { a == 12 }
    testTrue { a == "12" }
    testFalse { a != 12 }
    testFalse { a != "12" }
    testTrue { c == "text" }
    testTrue { d == 1 }
    testFalse { d == 0 }
    testFalse { c != "text" }
    testTrue { v == n }
    testTrue { tt == t }
    testFalse { v == bar }
    testTrue { n != bar }

    testFalse { a.equals(34) }
    testFalse { a.equals("34") }
    testTrue { a.equals(12) }
    testFalse { a.equals("12") }

    val ff: dynamic = f
    testFalse { ff.equals(v) }
    assertEquals(1, f.equalsCalled)

    return "OK"
}
