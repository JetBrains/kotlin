// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun foo() {
    val a = true
    val b = false
    val c = true
    <caret>if (a) {
        if (b && c) {
            doSomething("test")
        }
    }
}