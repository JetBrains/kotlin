// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-89048
// FIR_DUMP
// WITH_STDLIB

import lombok.Builder
import lombok.Singular

@Builder
class SingularConflict(
    val oneSheep: String,
    @Singular("oneSheep") val sheep: List<String>
)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
