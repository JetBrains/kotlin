// "Remove redundant label" "true"
fun foo() {
    L1@ val x = L2@<caret> bar()
}

fun bar() {}