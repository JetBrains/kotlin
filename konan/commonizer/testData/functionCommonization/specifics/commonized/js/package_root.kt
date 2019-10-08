actual suspend fun suspendFunction1() = 1
suspend fun suspendFunction2() = 1

actual class Qux actual constructor()

actual operator fun Qux.get(index: Int) = "$index"
actual operator fun Qux.set(index: Int, value: String) = Unit

actual infix fun Qux.infixFunction1(another: Qux) {}
actual infix fun Qux.infixFunction2(another: Qux) {}

actual tailrec fun tailrecFunction1() {}
actual tailrec fun tailrecFunction2() {}

actual external fun externalFunction1()
actual external fun externalFunction2()

actual inline fun inlineFunction1() {}
actual inline fun inlineFunction2() {}
