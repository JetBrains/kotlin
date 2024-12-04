fun functionNoParameters() {}
fun functionSingleParameter(i: Int) {}
fun functionTwoParameters(i: Int, s: String) {}
fun functionThreeParameters(i: Int, s: String, l: List<Double>) {}

fun functionMismatchedParameterNames1(i: Int) {}

fun functionMismatchedParameterNames2(i: Int, s: String) {}
fun functionMismatchedParameterNames3(i: Int, s: String) {}
fun functionMismatchedParameterNames4(i: Int, s: String) {}

fun functionMismatchedParameterNames5(i: Int, s: String, l: List<Double>) {}
fun functionMismatchedParameterNames6(i: Int, s: String, l: List<Double>) {}
fun functionMismatchedParameterNames7(i: Int, s: String, l: List<Double>) {}
fun functionMismatchedParameterNames8(i: Int, s: String, l: List<Double>) {}
fun functionMismatchedParameterNames9(i: Int, s: String, l: List<Double>) {}
fun functionMismatchedParameterNames10(i: Int, s: String, l: List<Double>) {}
fun functionMismatchedParameterNames11(i: Int, s: String, l: List<Double>) {}

fun functionMismatchedParameterNames12(vararg v: Int) {}
fun functionMismatchedParameterNames13(i: Int, vararg v: Int) {}
fun functionMismatchedParameterNames14(i: Int, vararg v: Int) {}
fun functionMismatchedParameterNames15(i: Int, vararg v: Int) {}

fun functionMismatchedParameterNames16(i: Int) {}
fun functionMismatchedParameterNames17(i: Int, s: String) {}
fun functionMismatchedParameterNames18(i: Int, s: String, l: List<Double>) {}
fun functionMismatchedParameterNames19(vararg v: Int) {}
fun functionMismatchedParameterNames20(i: Int, vararg v: Int) {}

fun functionMismatchedParameterNames21(arg0: Int) {}
fun functionMismatchedParameterNames22(arg0: Int, arg1: String) {}
fun functionMismatchedParameterNames23(arg0: Int, arg1: String, arg2: List<Double>) {}
fun functionMismatchedParameterNames24(vararg variadicArguments: Int) {}
fun functionMismatchedParameterNames25(arg0: Int, vararg variadicArguments: Int) {}

fun functionMismatchedParameterNames26(arg0: Int) {}
fun functionMismatchedParameterNames27(arg0: Int, arg1: String) {}
fun functionMismatchedParameterNames28(arg0: Int, arg1: String, arg2: List<Double>) {}
fun functionMismatchedParameterNames29(vararg variadicArguments: Int) {}
fun functionMismatchedParameterNames30(arg0: Int, vararg variadicArguments: Int) {}

fun functionMismatchedParameterNames31(i: Int, s: String) {}

@kotlinx.cinterop.ObjCMethod("fmpn32:s:", "", false)
fun functionMismatchedParameterNames32(i: Int, s: String) {
}

fun functionMismatchedParameterNames33(i: Int, s: String) {}

@kotlinx.cinterop.ObjCMethod("fmpn34:s:", "", false)
fun functionMismatchedParameterNames34(i: Int, s: String) {
}

@kotlinx.cinterop.ObjCMethod("fmpn35:s:", "", false)
fun functionMismatchedParameterNames35(i: Int, s: String) {
}

@kotlinx.cinterop.ObjCMethod("fmpn36:arg1:", "", false)
fun functionMismatchedParameterNames36(arg0: Int, arg1: String) {
}

@kotlinx.cinterop.ObjCMethod("fmpn37:arg1:", "", false)
fun functionMismatchedParameterNames37(arg0: Int, arg1: String) {
}

// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod("fmpn38:s:", "", false)
fun functionMismatchedParameterNames38(i: Int, s: String) {
}

@kotlinx.cinterop.ObjCMethod("fmpn39:s:", "", false)
fun functionMismatchedParameterNames39(i: Int, s: String) {
}

// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod("fmpn40:s:", "", false)
fun functionMismatchedParameterNames40(i: Int, s: String) {
}

// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod("fmpn41:arg1:", "", false)
fun functionMismatchedParameterNames41(arg0: Int, arg1: String) {
}

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

fun functionMismatchedParameterCount1(i: Int, s: String) {}
fun functionMismatchedParameterCount2(i: Int, s: String, l: List<Double>) {}

fun functionMismatchedParameterTypes1(i: Int, s: String) {}
fun functionMismatchedParameterTypes2(i: Int, s: String) {}

fun functionDefaultValues1(i: Int = 1, s: String) {}
fun functionDefaultValues2(i: Int, s: String = "hello") {}

inline fun inlineFunction1(lazyMessage: () -> String) {}
inline fun inlineFunction2(lazyMessage: () -> String) {}
inline fun inlineFunction3(crossinline lazyMessage: () -> String) {}
inline fun inlineFunction4(lazyMessage: () -> String) {}
inline fun inlineFunction5(noinline lazyMessage: () -> String) {}

fun functionWithVararg1(vararg numbers: Int) {}
fun functionWithVararg2(vararg numbers: Int) {}
fun functionWithVararg3(vararg numbers: Int) {}
fun functionWithVararg4(numbers: Array<Int>) {}
fun functionWithVararg5(numbers: Array<out Int>) {}
fun functionWithVararg6(vararg names: String) {}
fun functionWithVararg7(vararg names: String) {}
fun functionWithVararg8(vararg names: String) {}
fun functionWithVararg9(names: Array<String>) {}
fun functionWithVararg10(names: Array<out String>) {}

fun <T> functionWithTypeParameters1(p1: T, p2: String) {}
fun <T> functionWithTypeParameters2(p1: T, p2: String) {}
fun <T> functionWithTypeParameters3(p1: T, p2: String) {}
fun <Q, R> functionWithTypeParameters4(p1: Q, p2: R) {}
fun <Q, R> functionWithTypeParameters5(p1: Q, p2: R) {}
fun <Q, R> functionWithTypeParameters6(p1: Q, p2: R) {}
