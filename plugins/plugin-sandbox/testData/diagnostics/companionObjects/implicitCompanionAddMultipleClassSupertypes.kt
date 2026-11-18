// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

open class Base {
    fun value(): String = "OK"
}

@WithCompanion(Base::class, Base::class)
class <!MANY_CLASSES_IN_SUPERTYPE_LIST, SUPERTYPE_APPEARS_TWICE!>Owner<!> {
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, stringLiteral */
