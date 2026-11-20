// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89739
// WITH_STDLIB

import lombok.Builder
import lombok.Singular

enum class Kind @Builder constructor(<!UNSUPPORTED_SINGULAR_TYPE!>@Singular("number")<!> val numbers: IntArray) {
    A(IntArray(0));
}

enum class OtherKind {
    B(IntArray(0));

    @Builder
    constructor(<!UNSUPPORTED_SINGULAR_TYPE!>@Singular("number")<!> numbers: IntArray)
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, primaryConstructor, propertyDeclaration, secondaryConstructor,
stringLiteral */
