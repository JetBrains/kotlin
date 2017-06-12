// EXPECTED_REACHABLE_NODES: 513
// See KT-6203
package foo

class TestClass {
    object a {
        override fun toString() = "a"
    }

    companion object {
        object b {
            override fun toString() = "b"
        }
    }

    fun test() = convert(a) + convert(b)
}

fun convert(o: Any) = o.toString()

fun box(): String {
    assertEquals("ab", TestClass().test())
    assertEquals("ab2", convert(TestClass.a) + convert(TestClass.Companion.b) + "2")

    return "OK"
}