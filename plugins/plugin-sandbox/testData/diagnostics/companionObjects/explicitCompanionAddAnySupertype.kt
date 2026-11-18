// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

@WithCompanion(Any::class)
class Owner {
    companion <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>object<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, objectDeclaration */
