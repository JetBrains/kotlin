fun <T> doSomething(a: T) {}

fun foo() {
    val a = true
    val b = false
    val c = true
    if (a <caret>|| b && c) {
        doSomething("test")
    }
}
