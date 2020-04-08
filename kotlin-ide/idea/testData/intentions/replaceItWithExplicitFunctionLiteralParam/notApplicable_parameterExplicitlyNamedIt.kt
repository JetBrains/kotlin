// IS_APPLICABLE: false
fun foo(i: (Int) -> Int) = 0
val x = foo { it -> i<caret>t + 1 }
