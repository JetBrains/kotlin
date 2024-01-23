fun functionNoParameters() {}
fun functionSingleParameter(i: Int) {}
fun functionTwoParameters(i: Int, s: String) {}
fun functionThreeParameters(i: Int, s: String, l: List<Double>) {}

fun functionMismatchedParameterNames1(xi: Int) {}

fun functionMismatchedParameterNames2(xi: Int, s: String) {}
fun functionMismatchedParameterNames3(i: Int, xs: String) {}
fun functionMismatchedParameterNames4(xi: Int, xs: String) {}

fun functionMismatchedParameterNames5(xi: Int, s: String, l: List<Double>) {}
fun functionMismatchedParameterNames6(i: Int, xs: String, l: List<Double>) {}
fun functionMismatchedParameterNames7(i: Int, s: String, xl: List<Double>) {}
fun functionMismatchedParameterNames8(xi: Int, xs: String, l: List<Double>) {}
fun functionMismatchedParameterNames9(xi: Int, s: String, xl: List<Double>) {}
fun functionMismatchedParameterNames10(i: Int, xs: String, xl: List<Double>) {}
fun functionMismatchedParameterNames11(xi: Int, xs: String, xl: List<Double>) {}

fun functionMismatchedParameterNames12(vararg xv: Int) {}
fun functionMismatchedParameterNames13(xi: Int, vararg v: Int) {}
fun functionMismatchedParameterNames14(i: Int, vararg xv: Int) {}
fun functionMismatchedParameterNames15(xi: Int, vararg xv: Int) {}

fun functionMismatchedParameterNames16(arg0: Int) {}
fun functionMismatchedParameterNames17(arg0: Int, arg1: String) {}
fun functionMismatchedParameterNames18(arg0: Int, arg1: String, arg2: List<Double>) {}
fun functionMismatchedParameterNames19(vararg variadicArguments: Int) {}
fun functionMismatchedParameterNames20(arg0: Int, vararg variadicArguments: Int) {}

fun functionMismatchedParameterNames21(i: Int) {}
fun functionMismatchedParameterNames22(i: Int, s: String) {}
fun functionMismatchedParameterNames23(i: Int, s: String, l: List<Double>) {}
fun functionMismatchedParameterNames24(vararg v: Int) {}
fun functionMismatchedParameterNames25(i: Int, vararg v: Int) {}

fun functionMismatchedParameterNames26(arg0: Int) {}
fun functionMismatchedParameterNames27(arg0: Int, arg1: String) {}
fun functionMismatchedParameterNames28(arg0: Int, arg1: String, arg2: List<Double>) {}
fun functionMismatchedParameterNames29(vararg variadicArguments: Int) {}
fun functionMismatchedParameterNames30(arg0: Int, vararg variadicArguments: Int) {}

fun functionMismatchedParameterNames31(i: Int, s: String) {}
fun functionMismatchedParameterNames32(i: Int, s: String) {}

@kotlinx.cinterop.ObjCMethod("fmpn33:s:", "", false)
fun functionMismatchedParameterNames33(i: Int, s: String) {
}

@kotlinx.cinterop.ObjCMethod("fmpn34:s:", "", false)
fun functionMismatchedParameterNames34(i: Int, s: String) {
}

@kotlinx.cinterop.ObjCMethod("fmpn35:arg1:", "", false)
fun functionMismatchedParameterNames35(arg0: Int, arg1: String) {
}

@kotlinx.cinterop.ObjCMethod("fmpn36:s:", "", false)
fun functionMismatchedParameterNames36(i: Int, s: String) {
}

@kotlinx.cinterop.ObjCMethod("fmpn37:arg1:", "", false)
fun functionMismatchedParameterNames37(arg0: Int, arg1: String) {
}

@kotlinx.cinterop.ObjCMethod("fmpn38:s:", "", false)
fun functionMismatchedParameterNames38(i: Int, s: String) {
}

// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod("fmpn39:s:", "", false)
fun functionMismatchedParameterNames39(i: Int, s: String) {
}

// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod("fmpn40:s:", "", false)
fun functionMismatchedParameterNames40(i: Int, s: String) {
}

@kotlinx.cinterop.ObjCMethod("fmpn41:arg1:", "", false)
fun functionMismatchedParameterNames41(arg0: Int, arg1: String) {
}

// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod("fmpn42:arg1:", "", false)
fun functionMismatchedParameterNames42(arg0: Int, arg1: String) {
}

// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod("fmpn43:arg1:", "", false)
fun functionMismatchedParameterNames43(arg0: Int, arg1: String) {
}

@kotlinx.cinterop.ObjCMethod("ofpn:s:", "", false)
fun overloadedFunctionByParameterNames(i: Int, s: String) {
}

@kotlinx.cinterop.ObjCMethod("ofpn:xs:", "", false)
fun overloadedFunctionByParameterNames(xi: Int, xs: String) {
}

fun functionMismatchedParameterCount1(i: Int, s: String, l: List<Double>) {}
fun functionMismatchedParameterCount2(i: Int, s: String) {}

fun functionMismatchedParameterTypes1(i: Short, s: String) {}
fun functionMismatchedParameterTypes2(i: Int, s: CharSequence) {}

fun functionDefaultValues1(i: Int = 1, s: String) {}
fun functionDefaultValues2(i: Int, s: String = "hello") {}

inline fun inlineFunction1(lazyMessage: () -> String) {}
inline fun inlineFunction2(crossinline lazyMessage: () -> String) {}
inline fun inlineFunction3(crossinline lazyMessage: () -> String) {}
inline fun inlineFunction4(noinline lazyMessage: () -> String) {}
inline fun inlineFunction5(noinline lazyMessage: () -> String) {}

fun functionWithVararg1(vararg numbers: Int) {}
fun functionWithVararg2(numbers: Array<Int>) {}
fun functionWithVararg3(numbers: Array<out Int>) {}
fun functionWithVararg4(numbers: Array<out Int>) {}
fun functionWithVararg5(numbers: Array<out Int>) {}
fun functionWithVararg6(vararg names: String) {}
fun functionWithVararg7(names: Array<String>) {}
fun functionWithVararg8(names: Array<out String>) {}
fun functionWithVararg9(names: Array<out String>) {}
fun functionWithVararg10(names: Array<out String>) {}

fun <T> functionWithTypeParameters1(p1: T, p2: String) {}
fun <T> functionWithTypeParameters2(p1: String, p2: String) {}
fun <T> functionWithTypeParameters2(p1: String, p2: T) {}
fun <Q, R> functionWithTypeParameters4(p1: Q, p2: R) {}
fun <R, Q> functionWithTypeParameters5(p1: Q, p2: R) {}
fun <Q, R> functionWithTypeParameters6(p1: R, p2: Q) {}
