// PROBLEM: none
// WITH_RUNTIME
fun test() {
    <caret>with ("") {
        this@with.length
    }
}