// "Replace with 'newFun(p)'" "true"

class X {
    @Deprecated("", ReplaceWith("newFun(p)"))
    fun oldFun(p: Int) {
        newFun(p)
    }

    fun newFun(p: Int){}
}

fun foo(x: X) {
    x/*receiver*/.<caret>oldFun(1/*parameter*/)
}
