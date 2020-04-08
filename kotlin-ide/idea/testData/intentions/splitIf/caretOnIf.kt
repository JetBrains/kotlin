fun <T> doSomething(a: T) {}

fun foo() {
    val a = true
    val b = false
    <caret>if (a && b) {
        doSomething("test")
    }
}