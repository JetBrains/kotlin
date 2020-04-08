fun <T> doSomething(a: T) {}

fun foo() {
    val a = true
    val b = false
    val c = true
    val d = false
    if (a || b) {
        <caret>if (c || d) {
            doSomething("test")
        }
    }
}
