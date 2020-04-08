// "Replace with 'newFun(p, p)'" "true"

@Deprecated("", ReplaceWith("newFun(p, p)"))
fun oldFun(p: String) {
    newFun(p, p)
}

fun newFun(p1: String, p2: String){}

fun foo() {
    <caret>oldFun("x")
}
