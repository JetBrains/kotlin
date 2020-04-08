// IS_APPLICABLE: false
// LANGUAGE_VERSION: 1.3

fun foo(s: String, b: Boolean){}

fun bar() {
    foo(<caret>"", true)
}