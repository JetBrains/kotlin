fun <T> doSomething(a: T) {}

fun foo() {
    val a = true
    val b = false
    val c = true
    <caret>if (a && b || c) {
        doSomething("test")
    }
}