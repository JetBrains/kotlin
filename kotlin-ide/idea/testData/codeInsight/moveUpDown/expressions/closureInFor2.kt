// MOVE: up
fun foo() {
    for (i in run { 1..2 }) {
        <caret>run {
        }
    }
}