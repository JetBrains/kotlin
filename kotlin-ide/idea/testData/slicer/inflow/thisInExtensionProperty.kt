// FLOW: IN

val Any.extensionProp: Any
    get() = this

fun foo() {
    val <caret>x = "".extensionProp
}