// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

class Base {
}

@WithCompanion(Base::class)
class <!FINAL_SUPERTYPE!>Owner<!> {
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference */
