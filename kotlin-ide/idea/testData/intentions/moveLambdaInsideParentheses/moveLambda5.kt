// IS_APPLICABLE: true
fun foo() {
    bar() <caret>{ it }
}

fun bar(b: (Int) -> Int) {
    b(1)
}
