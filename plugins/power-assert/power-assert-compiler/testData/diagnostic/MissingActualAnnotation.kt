// RUN_PIPELINE_TILL: FRONTEND

// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

import kotlin.powerassert.*

@PowerAssert
expect fun explain(value: Any): String?

// MODULE: platform()()(common)
// FILE: platform.kt

import kotlin.powerassert.*

// TODO(KT-85237): Should report missing annotation here.
actual fun explain(value: Any): String? {
    return <!POWER_ASSERT_ILLEGAL_EXPLANATION_ACCESS!>PowerAssert.explanation<!>?.toDefaultMessage()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, elvisExpression, equalityExpression, expect, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, lambdaLiteral, localProperty, nullableType, outProjection, override,
primaryConstructor, propertyDeclaration, safeCall, stringLiteral, thisExpression, tryExpression, typeParameter, vararg */
