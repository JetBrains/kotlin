// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

open class Base {
    fun value(): String = "OK"
}

@WithCompanion(Base::class, Base::class)
class Owner {
    companion <!MANY_CLASSES_IN_SUPERTYPE_LIST, SUPERTYPE_APPEARS_TWICE!>object<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, functionDeclaration, objectDeclaration,
stringLiteral */
