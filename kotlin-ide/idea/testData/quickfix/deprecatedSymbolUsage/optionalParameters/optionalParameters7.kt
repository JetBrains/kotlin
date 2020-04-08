// "Replace with 'newFun(p1, p2, p3)'" "true"

interface I {
    @Deprecated("", ReplaceWith("newFun(p1, p2, p3)"))
    fun oldFun(p1: String, p2: String = bar(), p3: String = p2)

    fun newFun(p1: String, p2: String, p3: String)
}

fun foo(i: I) {
    i.<caret>oldFun("")
}

fun bar(): String = ""
