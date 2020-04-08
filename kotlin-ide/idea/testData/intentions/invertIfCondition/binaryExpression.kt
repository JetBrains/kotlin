fun foo(): Boolean {
    return true
}

fun main() {
    <caret>if (foo() && foo()) {
        foo()
    } else {
        foo()
    }
}