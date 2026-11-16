// RUN_PIPELINE_TILL: CODEGEN
// DUMP_SCHEMAS
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun test() {
    <!FUNCTION_SCHEMA!>fun createPair()<!> = run {
        val <!PROPERTY_SCHEMA!>df<!> = <!FUNCTION_CALL_SCHEMA!>dataFrameOf<!>("a" to columnOf(42))
        <!PROPERTY_ACCESS_SCHEMA!>df<!> <!FUNCTION_CALL_SCHEMA!>to<!> 1
    }

    val <!PROPERTY_SCHEMA!>res<!> = <!FUNCTION_CALL_SCHEMA!>createPair<!>()
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, integerLiteral, interfaceDeclaration,
isExpression, lambdaLiteral, localClass, localFunction, localProperty, nullableType, primaryConstructor,
propertyDeclaration, reified, samConversion, sealed, smartcast, stringLiteral, thisExpression, typeParameter,
whenExpression, whenWithSubject */
