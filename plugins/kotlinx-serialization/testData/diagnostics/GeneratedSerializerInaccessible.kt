// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import kotlinx.serialization.*

@Serializable
class A

fun foo() {
    A.<!DEPRECATION_ERROR!>`$serializer`<!>
    A.<!DEPRECATION_ERROR!>`$serializer`<!>.descriptor
    A(<!TOO_MANY_ARGUMENTS!>0<!>, <!TOO_MANY_ARGUMENTS!>null<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType */
