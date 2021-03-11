// WITH_RUNTIME
fun test() {
    listOf(listOf(1), listOf(2)).<caret>flatten().filter { it > 1 }
}