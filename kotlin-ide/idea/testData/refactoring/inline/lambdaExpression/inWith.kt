// ERROR: Inline Function refactoring cannot be applied to lambda expression without invocation

val xx = with(<caret>{ x: Int, y: Int -> x + y }) {
    invoke(1, 2)
}