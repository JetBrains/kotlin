// MOVE: up
fun foo(i: Int) {
    if (i in run { 1..2 }) {
        <caret>run {
        }
    }
}