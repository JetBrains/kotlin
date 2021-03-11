// PROBLEM: none
// WITH_RUNTIME
fun Array<List<Int>>.test() {
    <caret>flatMap { it }
}