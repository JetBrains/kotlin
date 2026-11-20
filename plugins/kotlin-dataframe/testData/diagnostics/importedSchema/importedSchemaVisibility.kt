// RUN_PIPELINE_TILL: FRONTEND
// WITH_SCHEMA_READER

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchemaSource
<!DATA_SCHEMA_DECLARATION_VISIBILITY!>private<!> interface A {
    val a: Int
}

@DataSchemaSource
internal interface B {
    val a: Int
}

@DataSchemaSource
public interface C {
    val a: Int
}

@DataSchemaSource
interface D {
    val a: Int
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, annotationDeclaration, classDeclaration, data, elvisExpression,
equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline,
interfaceDeclaration, isExpression, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration,
reified, samConversion, sealed, smartcast, starProjection, stringLiteral, thisExpression, typeParameter, whenExpression,
whenWithSubject */
