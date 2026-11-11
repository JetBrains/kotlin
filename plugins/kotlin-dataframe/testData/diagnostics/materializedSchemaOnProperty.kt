// RUN_PIPELINE_TILL: CODEGEN
// RENDER_DIAGNOSTIC_ARGUMENTS

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

class Context {
    private val <!DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE("@DataSchemadata class Df(    val a: Int,)")!>df<!> = dataFrameOf("a" to columnOf(1))
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, integerLiteral, interfaceDeclaration,
isExpression, lambdaLiteral, localClass, localProperty, nullableType, primaryConstructor, propertyDeclaration, reified,
samConversion, sealed, smartcast, stringLiteral, thisExpression, typeParameter, whenExpression, whenWithSubject */
