// "Replace with 'newFun<String>()'" "true"

@Deprecated("", ReplaceWith("newFun<T>()"))
fun <T> oldFun() {
    newFun<T>()
}

fun <T> newFun(){}

fun foo() {
    <caret>oldFun<String>()
}
