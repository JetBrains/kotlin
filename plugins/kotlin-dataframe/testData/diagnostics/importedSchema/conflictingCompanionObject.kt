// RUN_PIPELINE_TILL: FRONTEND
// WITH_SCHEMA_READER

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.columns.*

@DataSchemaSource
interface MySchema {
    companion <!CONFLICTING_COMPANION_OBJECT_DECLARATION!>object<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, annotationDeclaration, classDeclaration, companionObject, data,
elvisExpression, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline,
interfaceDeclaration, isExpression, lambdaLiteral, localProperty, nullableType, objectDeclaration, primaryConstructor,
propertyDeclaration, reified, samConversion, sealed, smartcast, starProjection, stringLiteral, thisExpression,
typeParameter, whenExpression, whenWithSubject */
