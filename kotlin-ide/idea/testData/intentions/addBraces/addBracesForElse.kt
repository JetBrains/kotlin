fun <T> doSomething(a: T) {}

fun foo() {
    if (true) doSomething("test")
    <caret>else doSomething("test2")
}
