// ERROR: Inline Function refactoring cannot be applied to anonymous function with receiver

val xx = (fu<caret>n Int.(x: Int, y: Int) = this + x + y)(1, 2, 3)