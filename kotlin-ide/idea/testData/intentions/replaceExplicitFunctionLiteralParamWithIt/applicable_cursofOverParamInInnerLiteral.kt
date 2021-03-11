fun foo(a: (Int) -> Int): Int = a(1)
val x = foo { p -> foo { y -> <caret>p } }