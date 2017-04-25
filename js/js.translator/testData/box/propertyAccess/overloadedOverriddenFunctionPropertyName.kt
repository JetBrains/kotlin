// EXPECTED_REACHABLE_NODES: 494
package foo

interface I {
    fun test(): String
}

class P : I {
    override fun test(): String {
        return "O" + test("K")
    }

    private fun test(p: String): String {
        return p
    }
}

fun box(): String {
    return P().test()
}