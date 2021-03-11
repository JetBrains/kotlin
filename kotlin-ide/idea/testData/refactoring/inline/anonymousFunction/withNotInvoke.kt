// ERROR: Inline Function refactoring cannot be applied to anonymous function without invocation

val xx = (fu<caret>n (x: Int, y: Int) = x + y).invsoke(1, 2)