actual suspend fun suspendFunction1() = 1
fun suspendFunction2() = 1

actual class Qux actual constructor()

actual operator fun Qux.get(index: Int) = "$index"
actual fun Qux.set(index: Int, value: String) = Unit

actual infix fun Qux.infixFunction1(another: Qux) {}
actual fun Qux.infixFunction2(another: Qux) {}

actual tailrec fun tailrecFunction1() {}
actual fun tailrecFunction2() {}

actual external fun externalFunction1()
actual fun externalFunction2() {}

actual inline fun inlineFunction1() {}
actual fun inlineFunction2() {}
