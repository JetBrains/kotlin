fun <T> doSomething(a: T) {}

fun foo() {
    do {
        doSomething("test")
    <caret>} while(true)
}
