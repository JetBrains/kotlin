// "Replace with 'newFun(option1, option2, option3, null)'" "true"

interface I {
    @Deprecated("", ReplaceWith("newFun(option1, option2, option3, null)"))
    fun oldFun(option1: String = "", option2: Int = 0, option3: Int = -1)

    fun newFun(option1: String = "", option2: Int = 0, option3: Int = -1, option4: String? = "x")
}

fun foo(i: I) {
    i.<caret>oldFun(option2 = 1)
}
