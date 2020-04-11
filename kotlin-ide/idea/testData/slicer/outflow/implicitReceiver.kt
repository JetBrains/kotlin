// FLOW: OUT

fun Any.extensionFun(): Any {
    return this
}

fun String.foo() {
    val v = extensionFun()
}

fun main() {
    <caret>"A".foo()
}
