actual fun functionNoParameters() {}
actual fun functionSingleParameter(i: Int) {}
actual fun functionTwoParameters(i: Int, s: String) {}
actual fun functionThreeParameters(i: Int, s: String, l: List<Double>) {}

// hasStableParameterNames=false
actual fun functionMismatchedParameterNames1(arg0: Int) {}

// hasStableParameterNames=false
actual fun functionMismatchedParameterNames2(arg0: Int, arg1: String) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames3(arg0: Int, arg1: String) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames4(arg0: Int, arg1: String) {}

// hasStableParameterNames=false
actual fun functionMismatchedParameterNames5(arg0: Int, arg1: String, arg2: List<Double>) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames6(arg0: Int, arg1: String, arg2: List<Double>) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames7(arg0: Int, arg1: String, arg2: List<Double>) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames8(arg0: Int, arg1: String, arg2: List<Double>) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames9(arg0: Int, arg1: String, arg2: List<Double>) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames10(arg0: Int, arg1: String, arg2: List<Double>) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames11(arg0: Int, arg1: String, arg2: List<Double>) {}

// hasStableParameterNames=false
actual fun functionMismatchedParameterNames12(vararg variadicArguments: Int) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames13(arg0: Int, vararg variadicArguments: Int) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames14(arg0: Int, vararg variadicArguments: Int) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames15(arg0: Int, vararg variadicArguments: Int) {}

actual fun functionMismatchedParameterNames16(i: Int) {}
actual fun functionMismatchedParameterNames17(i: Int, s: String) {}
actual fun functionMismatchedParameterNames18(i: Int, s: String, l: List<Double>) {}
actual fun functionMismatchedParameterNames19(vararg v: Int) {}
actual fun functionMismatchedParameterNames20(i: Int, vararg v: Int) {}

// hasStableParameterNames=false
actual fun functionMismatchedParameterNames21(i: Int) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames22(i: Int, s: String) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames23(i: Int, s: String, l: List<Double>) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames24(vararg v: Int) {}
// hasStableParameterNames=false
actual fun functionMismatchedParameterNames25(i: Int, vararg v: Int) {}

actual fun functionMismatchedParameterNames26(arg0: Int) {}
actual fun functionMismatchedParameterNames27(arg0: Int, arg1: String) {}
actual fun functionMismatchedParameterNames28(arg0: Int, arg1: String, arg2: List<Double>) {}
actual fun functionMismatchedParameterNames29(vararg variadicArguments: Int) {}
actual fun functionMismatchedParameterNames30(arg0: Int, vararg variadicArguments: Int) {}

actual fun functionMismatchedParameterNames31(i: Int, s: String) {}
@kotlinx.cinterop.ObjCMethod
fun functionMismatchedParameterNames32(i: Int, s: String) {}
fun functionMismatchedParameterNames33(i: Int, s: String) {}
@kotlinx.cinterop.ObjCMethod
actual fun functionMismatchedParameterNames34(i: Int, s: String) {}

@kotlinx.cinterop.ObjCMethod
fun functionMismatchedParameterNames35(i: Int, s: String) {}
@kotlinx.cinterop.ObjCMethod
fun functionMismatchedParameterNames36(arg0: Int, arg1: String) {}
@kotlinx.cinterop.ObjCMethod
actual fun functionMismatchedParameterNames37(arg0: Int, arg1: String) {}

// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod
actual fun functionMismatchedParameterNames38(i: Int, s: String) {}
@kotlinx.cinterop.ObjCMethod
actual fun functionMismatchedParameterNames39(i: Int, s: String) {}
// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod
actual fun functionMismatchedParameterNames40(i: Int, s: String) {}

// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod
actual fun functionMismatchedParameterNames41(arg0: Int, arg1: String) {}
@kotlinx.cinterop.ObjCMethod
actual fun functionMismatchedParameterNames42(arg0: Int, arg1: String) {}
// hasStableParameterNames=false
@kotlinx.cinterop.ObjCMethod
actual fun functionMismatchedParameterNames43(arg0: Int, arg1: String) {}

@kotlinx.cinterop.ObjCMethod
actual fun overloadedFunctionByParameterNames(i: Int, s: String) {}
@kotlinx.cinterop.ObjCMethod
actual fun overloadedFunctionByParameterNames(xi: Int, xs: String) {}

fun functionMismatchedParameterCount1(i: Int, s: String) {}
fun functionMismatchedParameterCount2(i: Int, s: String, l: List<Double>) {}

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
