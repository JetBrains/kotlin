expect fun functionNoParameters()
expect fun functionSingleParameter(i: Int)
expect fun functionTwoParameters(i: Int, s: String)
expect fun functionThreeParameters(i: Int, s: String, l: List<Double>)

expect inline fun inlineFunction1(lazyMessage: () -> String)
expect inline fun inlineFunction2(lazyMessage: () -> String)
expect inline fun inlineFunction3(crossinline lazyMessage: () -> String)
expect inline fun inlineFunction4(lazyMessage: () -> String)
expect inline fun inlineFunction5(noinline lazyMessage: () -> String)

expect fun functionWithVararg1(vararg numbers: Int)
expect fun functionWithVararg5(numbers: Array<out Int>)
expect fun functionWithVararg6(vararg names: String)
expect fun functionWithVararg10(names: Array<out String>)

expect fun <T> functionWithTypeParameters1(p1: T, p2: String)
expect fun <Q, R> functionWithTypeParameters4(p1: Q, p2: R)
