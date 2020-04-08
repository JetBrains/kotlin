// "Replace with 'newFun()'" "true"
// WITH_RUNTIME
package ppp

fun bar(): Int = 0

@Deprecated("", ReplaceWith("newFun()"))
fun oldFun(p: Int = ppp.bar()) {
    newFun()
}

fun newFun(){}

fun foo() {
    <caret>oldFun()
}
