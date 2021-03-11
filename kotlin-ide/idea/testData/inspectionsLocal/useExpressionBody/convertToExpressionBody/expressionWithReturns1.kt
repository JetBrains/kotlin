// PROBLEM: none

fun foo(p: Boolean): String {
    <caret>return bar() ?: return "a"
}

fun bar(): String? = null