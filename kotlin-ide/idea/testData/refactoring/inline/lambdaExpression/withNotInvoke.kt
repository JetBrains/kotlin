// ERROR: Inline Function refactoring cannot be applied to lambda expression without invocation

val xx = <caret>{ x: Int, y: Int -> x + y }.invsoke(1, 2)