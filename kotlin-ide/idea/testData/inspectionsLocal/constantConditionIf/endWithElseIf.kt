// WITH_RUNTIME

fun foo(x: Int) {}

fun bar(s: String?) {
    if (s == null) {
        1
    }
    else if (<caret>true) {
        2
    }
}