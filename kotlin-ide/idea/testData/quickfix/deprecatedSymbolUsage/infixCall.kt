// "Replace with 'newFun(p, this)'" "true"
// ERROR: 'infix' modifier is inapplicable on this function: must be a member or an extension function

@Deprecated("", ReplaceWith("newFun(p, this)"))
infix fun String.oldFun(p: Int) {
    newFun(p, this)
}

infix fun newFun(p: Int, s: String){}

fun foo() {
    "" <caret>oldFun 1
}
