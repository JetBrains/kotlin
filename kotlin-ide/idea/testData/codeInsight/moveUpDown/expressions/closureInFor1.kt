// MOVE: down
fun foo() {
    <caret>run {
    }
    for (i in run { 1..2 }) {
    }
}