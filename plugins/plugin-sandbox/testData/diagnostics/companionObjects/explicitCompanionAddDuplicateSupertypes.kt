// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

interface Base {
    fun value(): String = "OK"
}

@WithCompanion(Base::class, Base::class)
class Owner {
    companion <!SUPERTYPE_APPEARS_TWICE!>object<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, functionDeclaration, interfaceDeclaration,
objectDeclaration, stringLiteral */
