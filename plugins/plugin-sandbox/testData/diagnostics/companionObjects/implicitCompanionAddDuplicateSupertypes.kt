// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

interface Base {
    fun value(): String = "OK"
}

@WithCompanion(Base::class, Base::class)
class <!SUPERTYPE_APPEARS_TWICE!>Owner<!> {
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, stringLiteral */
