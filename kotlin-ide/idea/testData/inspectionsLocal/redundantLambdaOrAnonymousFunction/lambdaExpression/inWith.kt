// PROBLEM: none
// WITH_RUNTIME

val xx = with(<caret>{ x: Int, y: Int -> x + y }) {
    invoke(1, 2)
}