// "Replace with 'newFun(p, this)'" "true"

interface I

@Deprecated("", ReplaceWith("newFun(p, this)"))
operator fun I.plus(p: Int) {
    newFun(p, this)
}

fun newFun(p: Int, i: I) { }

fun foo(i: I) {
    i <caret>+ 1
}
