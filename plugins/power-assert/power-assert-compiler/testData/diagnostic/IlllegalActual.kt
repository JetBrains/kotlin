// RUN_PIPELINE_TILL: FRONTEND

// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect fun foo(bar: String): Unit

// MODULE: platform()()(common)
// FILE: platform.kt

import kotlin.powerassert.*

<!POWER_ASSERT_ILLEGAL_ACTUAL!>@PowerAssert<!>
actual fun foo(bar: String) {}

/* GENERATED_FIR_TAGS: actual, classDeclaration, elvisExpression, equalityExpression, expect, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, lambdaLiteral, localProperty, nullableType, outProjection, override,
primaryConstructor, propertyDeclaration, stringLiteral, thisExpression, tryExpression, typeParameter, vararg */
