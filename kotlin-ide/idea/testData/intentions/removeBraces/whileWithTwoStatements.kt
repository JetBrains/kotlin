// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun foo() {
    while (true) {
        doSomething("test")
        doSomething("test2")
    <caret>}
}
