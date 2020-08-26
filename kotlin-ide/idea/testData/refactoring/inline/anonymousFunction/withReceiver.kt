// ERROR: Inline Function refactoring cannot be applied to anonymous function with receiver
val xx = 1.(fu<caret>n Int.(x: Int, y: Int) = this + x + y)(2, 3)