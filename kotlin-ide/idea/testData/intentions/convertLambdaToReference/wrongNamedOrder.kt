// IS_APPLICABLE: false

fun foo(x: Int, y: Int) = x + y

val x = { x: Int, y: Int <caret>-> foo(y = x, x = y) }