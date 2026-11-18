// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

object Base {
}

@WithCompanion(Base::class)
class <!SINGLETON_IN_SUPERTYPE!>Owner<!> {
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, objectDeclaration */
