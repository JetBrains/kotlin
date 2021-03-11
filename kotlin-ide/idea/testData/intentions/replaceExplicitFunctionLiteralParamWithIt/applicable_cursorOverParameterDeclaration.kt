fun <A> applyTwice(f: (A) -> A, x: A) = f(f(x))
val x = applyTwice({ <caret>x -> 1 + x }, 40)
