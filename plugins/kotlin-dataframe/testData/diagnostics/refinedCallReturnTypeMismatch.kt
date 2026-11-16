// RUN_PIPELINE_TILL: FRONTEND
package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*

// KDF#1490: a type mismatch of a refined call has to be reported on the call instead of crashing the checker

@DataSchema
interface Type {
    val a: Int
}

fun test(): DataFrame<Type> {
    return <!RETURN_TYPE_MISMATCH!>dataFrameOf("a" to columnOf(1)).cast<Type>().add("b") { a }<!>
}

fun testExpressionBody(): DataFrame<Type> = <!RETURN_TYPE_MISMATCH!>dataFrameOf("a" to columnOf(1)).cast<Type>().add("b") { a }<!>

fun testWithVariable(): DataFrame<Type> {
    val df = dataFrameOf("a" to columnOf(1)).cast<Type>().add("b") { a }
    return <!RETURN_TYPE_MISMATCH!>df<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, integerLiteral, interfaceDeclaration,
isExpression, lambdaLiteral, localClass, localProperty, nullableType, primaryConstructor, propertyDeclaration, reified,
samConversion, sealed, smartcast, stringLiteral, thisExpression, typeParameter, whenExpression, whenWithSubject */
