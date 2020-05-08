suspend fun suspendFunction1() = 1
fun suspendFunction2() = 1

class Qux

operator fun Qux.get(index: Int) = "$index"
fun Qux.set(index: Int, value: String) = Unit

infix fun Qux.infixFunction1(another: Qux) {}
fun Qux.infixFunction2(another: Qux) {}

tailrec fun tailrecFunction1() {}
fun tailrecFunction2() {}

external fun externalFunction1()
fun externalFunction2() {}

inline fun inlineFunction1() {}
fun inlineFunction2() {}
