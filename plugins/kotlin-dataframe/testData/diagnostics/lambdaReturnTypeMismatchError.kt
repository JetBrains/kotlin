// RUN_PIPELINE_TILL: FRONTEND
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun test() {
    val df = dataFrameOf("a" to columnOf(1, null))
    df.select { <!RETURN_TYPE_MISMATCH!>"a"<!> }
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, integerLiteral, interfaceDeclaration,
isExpression, lambdaLiteral, localClass, localProperty, nullableType, primaryConstructor, propertyDeclaration, reified,
samConversion, sealed, smartcast, starProjection, stringLiteral, thisExpression, typeParameter, whenExpression,
whenWithSubject */
