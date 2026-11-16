// RUN_PIPELINE_TILL: FRONTEND
import org.jetbrains.kotlinx.dataframe.annotations.*

fun main() {
    @DataSchema
    data <!DATA_SCHEMA_LOCAL_DECLARATION!>class Local<!>(val a: Int)
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, interfaceDeclaration, isExpression,
lambdaLiteral, localClass, localProperty, nullableType, primaryConstructor, propertyDeclaration, reified, samConversion,
sealed, smartcast, stringLiteral, thisExpression, typeParameter, whenExpression, whenWithSubject */
