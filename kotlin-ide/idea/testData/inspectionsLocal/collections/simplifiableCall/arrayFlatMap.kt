// PROBLEM: none
// WITH_RUNTIME
fun test() {
    arrayOf(listOf(1), listOf(2)).flatMap<caret> { it }
}