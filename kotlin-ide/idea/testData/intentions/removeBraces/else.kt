fun <T> doSomething(a: T) {}

fun foo() {
    if (true) {
        doSomething("test")
    } else <caret>{
        doSomething("test2")
    }
}
