// RUN_PIPELINE_TILL: CODEGEN
// WITH_SCHEMA_READER

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.columns.*

@DataSchemaSource
interface MySchema

fun test() {
    DataFrame.empty(nrow = 1).cast<MySchema>()
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, annotationDeclaration, classDeclaration, data, elvisExpression,
equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, integerLiteral,
interfaceDeclaration, isExpression, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration,
reified, samConversion, sealed, smartcast, starProjection, stringLiteral, thisExpression, typeParameter, whenExpression,
whenWithSubject */
