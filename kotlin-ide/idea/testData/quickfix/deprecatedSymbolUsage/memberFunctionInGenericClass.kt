// "Replace with 'newFun(t)'" "true"

interface X<T> {
    @Deprecated("", ReplaceWith("newFun(t)"))
    fun oldFun(t: T)

    fun newFun(t: T)
}

fun foo(x: X<String>) {
    x.<caret>oldFun("a")
}
