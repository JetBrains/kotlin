// PROBLEM: none

fun foo(f: (Int) -> Unit) {}

fun bar() {
    foo { i <caret>-> }
}