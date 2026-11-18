// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

object Base {
}

@WithCompanion(Base::class)
class Owner {
    companion <!SINGLETON_IN_SUPERTYPE!>object<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, objectDeclaration */
