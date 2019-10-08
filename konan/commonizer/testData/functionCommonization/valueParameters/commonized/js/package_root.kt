actual fun functionNoParameters() {}
actual fun functionSingleParameter(i: Int) {}
actual fun functionTwoParameters(i: Int, s: String) {}
actual fun functionThreeParameters(i: Int, s: String, l: List<Double>) {}

fun functionMismatchedParameterCount1(i: Int, s: String) {}
fun functionMismatchedParameterCount2(i: Int, s: String, l: List<Double>) {}

fun functionMismatchedParameterNames1(i: Int, s: String) {}
fun functionMismatchedParameterNames2(i: Int, s: String) {}

fun functionMismatchedParameterTypes1(i: Int, s: String) {}
fun functionMismatchedParameterTypes2(i: Int, s: String) {}

fun functionDefaultValues1(i: Int = 1, s: String) {}
fun functionDefaultValues2(i: Int, s: String = "hello") {}

actual inline fun inlineFunction1(lazyMessage: () -> String) {}
actual inline fun inlineFunction2(lazyMessage: () -> String) {}
actual inline fun inlineFunction3(crossinline lazyMessage: () -> String) {}
actual inline fun inlineFunction4(lazyMessage: () -> String) {}
actual inline fun inlineFunction5(noinline lazyMessage: () -> String) {}

actual fun functionWithVararg1(vararg numbers: Int) {}
fun functionWithVararg2(vararg numbers: Int) {}
fun functionWithVararg3(vararg numbers: Int) {}
fun functionWithVararg4(numbers: Array<Int>) {}
actual fun functionWithVararg5(numbers: Array<out Int>) {}
actual fun functionWithVararg6(vararg names: String) {}
fun functionWithVararg7(vararg names: String) {}
fun functionWithVararg8(vararg names: String) {}
fun functionWithVararg9(names: Array<String>) {}
actual fun functionWithVararg10(names: Array<out String>) {}

actual fun <T> functionWithTypeParameters1(p1: T, p2: String) {}
fun <T> functionWithTypeParameters2(p1: T, p2: String) {}
fun <T> functionWithTypeParameters3(p1: T, p2: String) {}
actual fun <Q, R> functionWithTypeParameters4(p1: Q, p2: R) {}
fun <Q, R> functionWithTypeParameters5(p1: Q, p2: R) {}
fun <Q, R> functionWithTypeParameters6(p1: Q, p2: R) {}
