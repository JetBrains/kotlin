// RUN_PIPELINE_TILL: FRONTEND

import kotlinx.powerassert.*

fun powerAssert() {
    <!POWER_ASSERT_ILLEGAL_EXPLANATION_ACCESS!>PowerAssert.explanation<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, elvisExpression, equalityExpression, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, lambdaLiteral, localProperty, nullableType, outProjection, override,
primaryConstructor, propertyDeclaration, stringLiteral, thisExpression, tryExpression, typeParameter, vararg */
