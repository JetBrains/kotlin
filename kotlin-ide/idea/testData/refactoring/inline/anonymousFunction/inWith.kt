// ERROR: Inline Function refactoring cannot be applied to anonymous function without invocation

val xx = with(fu<caret>n(x: Int, y: Int) = x + y) {
    invoke(1, 2)
}