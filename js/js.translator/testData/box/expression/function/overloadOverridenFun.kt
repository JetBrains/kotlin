// EXPECTED_REACHABLE_NODES: 499
// KT-2219 if function overload overridden function its name doesn't translated correctly

package foo

interface I {
    fun test(): String
}

class P : I {
    override fun test(): String = "foo" + test("bar")

    private fun test(p: String) = p

    fun test(s: String, i: Int) = "$i $s"
}

fun box(): String {
    assertEquals("foobar", P().test())
    assertEquals("35 baz", P().test("baz", 35))

    return "OK"
}