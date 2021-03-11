fun <A> applyTwice(f: (A) -> A, x: A) = f(f(x))
val x = applyTwice({ i<caret>t + 1 }, 40)
