// RUN_PIPELINE_TILL: BACKEND

// MODULE: lib
// FILE: A.kt

import kotlinx.powerassert.*

@PowerAssert
fun describe(value: Any): String? {
    return PowerAssert.explanation?.toDefaultMessage()
}

// MODULE: main(lib)
// DISABLE_RUNTIME
// FILE: B.kt

fun box(): String {
    val reallyLongList = listOf("a", "b")
    return <!POWER_ASSERT_RUNTIME_UNAVAILABLE!>describe(reallyLongList.reversed() == emptyList<String>())<!> ?: "FAIL"
}

/* GENERATED_FIR_TAGS: classDeclaration, elvisExpression, equalityExpression, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, lambdaLiteral, localProperty, nullableType, outProjection, override,
primaryConstructor, propertyDeclaration, safeCall, stringLiteral, thisExpression, tryExpression, typeParameter, vararg */
