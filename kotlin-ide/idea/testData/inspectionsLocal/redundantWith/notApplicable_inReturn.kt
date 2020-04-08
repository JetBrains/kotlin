// PROBLEM: none
// WITH_RUNTIME
fun test(): Int {
    return <caret>with ("") {
        println()
        1
    }
}