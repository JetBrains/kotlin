// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89048
// FIR_DUMP
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB

import lombok.Builder
import lombok.Singular

@Builder
class <!LOMBOK_GENERATED_DECLARATION_CLASH!>SingularConflict<!>(
    val oneSheep: String,
    @Singular("oneSheep") val sheep: List<String>
)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
