// "Replace with 'newFun(p1, p2)'" "true"

interface I {
    @Deprecated("", ReplaceWith("newFun(p1, p2)"))
    fun oldFun(p1: String, p2: () -> Boolean)

    fun newFun(p1: String, p2: () -> Boolean, p3: String? = null)
}

fun foo(i: I) {
    i.<caret>oldFun("a") { true }
}
