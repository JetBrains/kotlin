// "Add 'toString()' call" "true"
// ACTION: Add 'toString()' call

fun foo() {
    bar(null as Any?<caret>)
}

fun bar(a: String) {
}