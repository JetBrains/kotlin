fun <T> doSomething(a: T) {}

fun foo() {
    <caret>while (true) {
        doSomething("test")
    }
}
