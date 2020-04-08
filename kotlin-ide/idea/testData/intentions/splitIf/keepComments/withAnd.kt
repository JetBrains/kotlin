fun <T> doSomething(a: T) {}

fun foo(p: Int) {
    <caret>if (0 < p /* > 0 */ && p < 100 /* not too much */) {
        doSomething("test")
    }
}
