// "Replace with 'newFun(p1, p3, p2)'" "true"

interface I {
    @Deprecated("", ReplaceWith("newFun(p1, p3, p2)"))
    fun oldFun(p1: String, p2: String = "", p3: Int = -1)

    fun newFun(p1: String, p2: Int = -1, p3: String = "")
}

fun foo(i: I) {
    i.<caret>oldFun("")
}
