// RUN_PIPELINE_TILL: FRONTEND
package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.explode

@DataSchema
interface ExplodeSchema {
    val timestamps: List<Int>
}

fun explode(df: DataFrame<ExplodeSchema>) {
    df.explode { <!UNRESOLVED_REFERENCE!>timestamp<!> }
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, interfaceDeclaration, isExpression,
lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, reified, samConversion, sealed,
smartcast, starProjection, stringLiteral, thisExpression, typeParameter, whenExpression, whenWithSubject */
