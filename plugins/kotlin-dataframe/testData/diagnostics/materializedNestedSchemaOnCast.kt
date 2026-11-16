// RUN_PIPELINE_TILL: CODEGEN
// RENDER_DIAGNOSTIC_ARGUMENTS

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val cities = dataFrameOf(
        "city" to columnOf("London", "Seoul"),
    )

    cities.<!MATERIALIZED_SCHEMA_ON_CAST("@DataSchemadata class MySchema(    val city: String,)")!>cast<!><Context.MySchema>()

    return "OK"
}

class Context {
    @DataSchema
    class MySchema
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, interfaceDeclaration, isExpression,
lambdaLiteral, localClass, localProperty, nestedClass, nullableType, primaryConstructor, propertyDeclaration, reified,
samConversion, sealed, smartcast, stringLiteral, thisExpression, typeParameter, whenExpression, whenWithSubject */
