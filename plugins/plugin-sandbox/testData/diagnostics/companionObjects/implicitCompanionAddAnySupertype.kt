// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

@WithCompanion(Any::class)
class <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>Owner<!> {
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference */
