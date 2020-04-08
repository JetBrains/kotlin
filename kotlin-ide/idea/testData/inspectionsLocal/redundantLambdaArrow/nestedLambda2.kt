// PROBLEM: none
// WITH_RUNTIME
fun test() {
    listOf("A").forEach {
        listOf("B").forEach { _ ->
            setOf(1).map { <caret>_ -> it.length }
        }
    }
}