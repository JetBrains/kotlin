// IS_APPLICABLE: true
fun foo() {
    bar<String> <caret>{ it.toString() }
}

fun <T> bar(a: (Int)->T): T {
    return a(1)
}
