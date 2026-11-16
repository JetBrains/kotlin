// RUN_PIPELINE_TILL: CODEGEN
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = DataFrame.Empty
    df.cast<Any>()
    return "OK"
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, interfaceDeclaration, isExpression,
lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, reified, samConversion, sealed,
smartcast, starProjection, stringLiteral, thisExpression, typeParameter, whenExpression, whenWithSubject */
