fun <T> doSomething(a: T) {}

fun foo(p: Int) {
    <caret>if (p < 0 /* p < 0 */ || p > 100 /* too much */) {
        doSomething("test")
    }
}
