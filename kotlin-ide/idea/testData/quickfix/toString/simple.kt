// "Add 'toString()' call" "true"

fun foo() {
    bar(Any()<caret>)
}

fun bar(a: String) {
}