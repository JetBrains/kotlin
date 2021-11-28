expect fun functionNoParameters()
expect fun functionSingleParameter(i: Int)
expect fun functionTwoParameters(i: Int, s: String)
expect fun functionThreeParameters(i: Int, s: String, l: List<Double>)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames1(arg0: Int)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames2(arg0: Int, arg1: String)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames3(arg0: Int, arg1: String)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames4(arg0: Int, arg1: String)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames5(arg0: Int, arg1: String, arg2: List<Double>)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames6(arg0: Int, arg1: String, arg2: List<Double>)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames7(arg0: Int, arg1: String, arg2: List<Double>)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames8(arg0: Int, arg1: String, arg2: List<Double>)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames9(arg0: Int, arg1: String, arg2: List<Double>)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames10(arg0: Int, arg1: String, arg2: List<Double>)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames11(arg0: Int, arg1: String, arg2: List<Double>)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames12(vararg variadicArguments: Int)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames13(arg0: Int, vararg variadicArguments: Int)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames14(arg0: Int, vararg variadicArguments: Int)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames15(arg0: Int, vararg variadicArguments: Int)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames16(i: Int)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames17(i: Int, s: String)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames18(i: Int, s: String, l: List<Double>)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames19(vararg v: Int)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames20(i: Int, vararg v: Int)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames21(i: Int)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames22(i: Int, s: String)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames23(i: Int, s: String, l: List<Double>)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames24(vararg v: Int)

// hasStableParameterNames=false
expect fun functionMismatchedParameterNames25(i: Int, vararg v: Int)

expect fun functionMismatchedParameterNames26(arg0: Int)
expect fun functionMismatchedParameterNames27(arg0: Int, arg1: String)
expect fun functionMismatchedParameterNames28(arg0: Int, arg1: String, arg2: List<Double>)
expect fun functionMismatchedParameterNames29(vararg variadicArguments: Int)
expect fun functionMismatchedParameterNames30(arg0: Int, vararg variadicArguments: Int)

expect fun functionMismatchedParameterNames31(i: Int, s: String)

@kotlin.commonizer.ObjCCallable
expect fun functionMismatchedParameterNames34(i: Int, s: String)

@kotlin.commonizer.ObjCCallable
expect fun functionMismatchedParameterNames37(arg0: Int, arg1: String)

// hasStableParameterNames=false
@kotlin.commonizer.ObjCCallable
expect fun functionMismatchedParameterNames38(i: Int, s: String)

// hasStableParameterNames=false
@kotlin.commonizer.ObjCCallable
expect fun functionMismatchedParameterNames39(i: Int, s: String)

// hasStableParameterNames=false
@kotlin.commonizer.ObjCCallable
expect fun functionMismatchedParameterNames40(i: Int, s: String)

// hasStableParameterNames=false
@kotlin.commonizer.ObjCCallable
expect fun functionMismatchedParameterNames41(arg0: Int, arg1: String)

// hasStableParameterNames=false
@kotlin.commonizer.ObjCCallable
expect fun functionMismatchedParameterNames42(arg0: Int, arg1: String)

// hasStableParameterNames=false
@kotlin.commonizer.ObjCCallable
expect fun functionMismatchedParameterNames43(arg0: Int, arg1: String)

@kotlin.commonizer.ObjCCallable
expect fun overloadedFunctionByParameterNames(i: Int, s: String)

@kotlin.commonizer.ObjCCallable
expect fun overloadedFunctionByParameterNames(xi: Int, xs: String)

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
