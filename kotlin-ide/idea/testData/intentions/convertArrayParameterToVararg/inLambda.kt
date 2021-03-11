// IS_APPLICABLE: false
// WITH_RUNTIME
fun test() {
    listOf(arrayOf(1)).map { <caret>i: Array<Int> ->
        i + 1
    }
}