// "Replace with 'newFun(p)'" "true"
// WITH_RUNTIME

fun foo(vararg s: String) = s.joinToString()

@Deprecated("", ReplaceWith("newFun(p)"))
fun oldFun(p: String){
    newFun(p)
}

fun newFun(p: String){}

fun foo() {
    <caret>oldFun(foo(*arrayOf("a", "b")))
}
