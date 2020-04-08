fun Int.foo(x: Int) = this - x

val x = { a: Int, b: Int <caret>-> a.foo(b) }