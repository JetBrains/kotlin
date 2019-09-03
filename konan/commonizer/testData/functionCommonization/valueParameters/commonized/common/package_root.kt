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
