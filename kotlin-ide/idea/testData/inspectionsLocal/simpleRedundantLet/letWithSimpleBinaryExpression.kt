// PROBLEM: none
// WITH_RUNTIME


fun foo() {
    "".let<caret> { it + 1 }
}