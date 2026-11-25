// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

class Base {
}

@WithCompanion(Base::class)
class Owner {
    companion <!FINAL_SUPERTYPE!>object<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, objectDeclaration */
