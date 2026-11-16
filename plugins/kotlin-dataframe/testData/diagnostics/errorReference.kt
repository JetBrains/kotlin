// RUN_PIPELINE_TILL: FRONTEND
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlin.random.Random

fun box(): String {
    val l: List<Int>? = if (Random.nextBoolean()) listOf(123) else null

    l<!UNSAFE_CALL!>.<!>toDataFrame {

    }
    return "OK"
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, integerLiteral, interfaceDeclaration,
isExpression, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, reified,
samConversion, sealed, smartcast, stringLiteral, thisExpression, typeParameter, whenExpression, whenWithSubject */
