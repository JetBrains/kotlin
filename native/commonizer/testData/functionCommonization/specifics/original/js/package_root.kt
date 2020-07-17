suspend fun suspendFunction1() = 1
suspend fun suspendFunction2() = 1

class Qux

operator fun Qux.get(index: Int) = "$index"
operator fun Qux.set(index: Int, value: String) = Unit

infix fun Qux.infixFunction1(another: Qux) {}
infix fun Qux.infixFunction2(another: Qux) {}

tailrec fun tailrecFunction1() {}
tailrec fun tailrecFunction2() {}

external fun externalFunction1()
external fun externalFunction2()

inline fun inlineFunction1() {}
inline fun inlineFunction2() {}
