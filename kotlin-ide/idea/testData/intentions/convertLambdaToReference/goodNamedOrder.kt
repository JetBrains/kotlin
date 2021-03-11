fun foo(x: Int, y: Int) = x + y

val x = { y: Int, x: Int <caret>-> foo(y = x, x = y) }