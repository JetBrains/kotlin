// RUN_PIPELINE_TILL: CODEGEN
// WITH_EXTRA_CHECKERS
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("group" to columnOf(
        "a" to columnOf(1),
    ))

    df.convert { group }.toInt().group[0]
    <!UNREACHABLE_CODE!>return "OK"<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, integerLiteral, interfaceDeclaration,
isExpression, lambdaLiteral, localClass, localProperty, nullableType, primaryConstructor, propertyDeclaration, reified,
samConversion, sealed, smartcast, stringLiteral, thisExpression, typeParameter, whenExpression, whenWithSubject */
