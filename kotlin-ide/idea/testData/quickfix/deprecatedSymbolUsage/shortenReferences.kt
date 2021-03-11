// "Replace with 'newFun(java.io.File.separatorChar)'" "true"

@Deprecated("", ReplaceWith("newFun(java.io.File.separatorChar)"))
fun oldFun() { }

fun newFun(separator: Char){}

fun foo() {
    <caret>oldFun()
}
