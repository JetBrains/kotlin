// PROBLEM: none

fun foo(vararg x: String) {}

fun bar() {
    // We do not want to apply inspection here, because named argument is lost
    foo(x = *arrayOf<caret>("abc"))
}
