fun foo(s: String, b: Boolean){}

fun bar() {
    foo("", b = <caret>true)
}