// "Replace with 'newFun(this, p1, p2)'" "true"

interface X {
    @Deprecated("", ReplaceWith("newFun(this, p1, p2)"))
    fun oldFun(p1: Int, p2: Int)
}

fun newFun(x: X, a: Int, b: Int){}

fun foo(x: X) {
    x/*receiver*/.<caret>oldFun(
        1, // pass 1
        2 // pass 2
    )
}
