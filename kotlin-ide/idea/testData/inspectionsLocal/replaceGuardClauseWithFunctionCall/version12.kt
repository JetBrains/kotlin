// PROBLEM: none
// WITH_RUNTIME
// LANGUAGE_VERSION: 1.2
fun test(foo: Int?) {
    <caret>if (foo == null) {
        throw IllegalArgumentException("test")
    }
    bar(foo)
}

fun bar(i: Int) {}