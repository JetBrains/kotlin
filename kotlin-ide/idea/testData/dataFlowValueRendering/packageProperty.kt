package foo

val a: Any? = null

fun outer() {
    if (a is String) {
        <caret>null
    }
}