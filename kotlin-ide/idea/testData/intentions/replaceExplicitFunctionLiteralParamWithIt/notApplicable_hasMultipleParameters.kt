// IS_APPLICABLE: false

fun call2(f: (Int, Int) -> Int, x: Int, y: Int) = f(x, y)
val foo = call2({ <caret>x, y -> x + y }, 40, 2)
