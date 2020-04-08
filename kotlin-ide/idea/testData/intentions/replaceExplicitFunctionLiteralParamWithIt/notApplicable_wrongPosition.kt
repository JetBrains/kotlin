// IS_APPLICABLE: false

fun <A> applyTwice(f: (A) -> A, x: A) = f(f(x))
val x = applyTwice({ p -> p + <caret>1 }, 40)
