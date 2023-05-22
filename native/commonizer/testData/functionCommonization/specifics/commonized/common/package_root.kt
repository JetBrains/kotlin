expect suspend fun suspendFunction1(): Int

expect class Qux()

expect operator fun Qux.get(index: Int): String
expect fun Qux.set(index: Int, value: String)

expect infix fun Qux.infixFunction1(another: Qux)
expect fun Qux.infixFunction2(another: Qux)

expect fun tailrecFunction1()
expect fun tailrecFunction2()

expect fun externalFunction1()
expect fun externalFunction2()

expect inline fun inlineFunction1() {}
expect fun inlineFunction2() {}
