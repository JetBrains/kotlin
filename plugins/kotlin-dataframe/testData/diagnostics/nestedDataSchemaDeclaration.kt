// RUN_PIPELINE_TILL: CODEGEN
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Person(
    val name: Name,
) {

    @DataSchema
    data class Name(
        val firstName: String,
        val lastName: String,
    )
}

fun test() {
    val df = dataFrameOf(Person(Person.Name("Alice", "Cooper")))

    df.name.firstName
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, interfaceDeclaration, isExpression,
lambdaLiteral, localProperty, nestedClass, nullableType, primaryConstructor, propertyDeclaration, reified, samConversion,
sealed, smartcast, stringLiteral, thisExpression, typeParameter, whenExpression, whenWithSubject */
