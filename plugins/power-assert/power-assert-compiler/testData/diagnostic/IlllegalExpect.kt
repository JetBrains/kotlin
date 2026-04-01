// RUN_PIPELINE_TILL: BACKEND

// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

import kotlin.powerassert.*

@PowerAssert
expect fun foo(bar: String): Unit

// MODULE: platform()()(common)
// FILE: platform.kt

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun foo(bar: String) {}

/* GENERATED_FIR_TAGS: actual, classDeclaration, elvisExpression, equalityExpression, expect, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, lambdaLiteral, localProperty, nullableType, outProjection, override,
primaryConstructor, propertyDeclaration, stringLiteral, thisExpression, tryExpression, typeParameter, vararg */
