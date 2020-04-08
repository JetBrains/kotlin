// "Replace with 'newFun()'" "true"
// WITH_RUNTIME

@Deprecated("", ReplaceWith("newFun()"))
fun oldFun(p: Int): Int {
    return newFun()
}

fun newFun(): Int = 0

fun foo(): Int {
    return <caret>oldFun(bar())
}

fun bar(): Int = 0
