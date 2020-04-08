fun <T> doSomething(a: T) {}

fun foo() {
    val a = true
    val b = false
    val c = true
    val d = false
    <caret>if ((a && b || c) || d) {
        doSomething("test")
    }
}
