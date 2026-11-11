// RUN_PIPELINE_TILL: FRONTEND
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
<!DATA_SCHEMA_DECLARATION_VISIBILITY!>private<!> interface A {
    val a: Int
}

@DataSchema
internal interface B {
    val a: Int
}

@DataSchema
public interface C {
    val a: Int
}

@DataSchema
interface D {
    val a: Int
}

open class Container {
    @DataSchema
    <!DATA_SCHEMA_DECLARATION_VISIBILITY!>protected<!> interface E
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, elvisExpression, equalityExpression,
funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection, inline, interfaceDeclaration, isExpression,
lambdaLiteral, localProperty, nestedClass, nullableType, primaryConstructor, propertyDeclaration, reified, samConversion,
sealed, smartcast, stringLiteral, thisExpression, typeParameter, whenExpression, whenWithSubject */
