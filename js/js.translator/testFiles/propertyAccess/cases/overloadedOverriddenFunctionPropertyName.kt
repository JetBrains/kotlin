package foo

trait I {
    fun test(): String
}

class P : I {
    override fun test(): String {
        return "a" + test("b")
    }

    private fun test(p: String): String {
        return p
    }
}

fun box(): Boolean {
    return P().test() == "ab"
}