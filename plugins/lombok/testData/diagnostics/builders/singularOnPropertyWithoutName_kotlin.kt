// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89739
// WITH_STDLIB

import lombok.Builder
import lombok.Singular

@Builder
class WithoutName {
    @Singular var<!SYNTAX!><!> : List<String> = emptyList()
}

/* GENERATED_FIR_TAGS: classDeclaration, propertyDeclaration */
