// PROBLEM: none
// WITH_RUNTIME
fun test() {
    val i = <caret>with ("") {
        println()
        1
    }
}