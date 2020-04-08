// IS_APPLICABLE: true
fun foo() {
    bar <caret>{ it }
}

fun bar(a: (Int) -> Int): Int {
    return a(1)
}
