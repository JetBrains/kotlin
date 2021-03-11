fun <T> doSomething(a: T) {}

fun foo() {
    for (i in 1..4)
    <caret>doSomething("test")
}
