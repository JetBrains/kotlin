// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91394
// FIR_DUMP

import lombok.Data

<!ANNOTATION_HAS_NO_EFFECT!>@Data(staticConstructor = "of")<!>
enum class A(val x: Int) {
    FIRST(1)
}

fun test() {
    A.<!UNRESOLVED_REFERENCE!>of<!>(1)
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral */
