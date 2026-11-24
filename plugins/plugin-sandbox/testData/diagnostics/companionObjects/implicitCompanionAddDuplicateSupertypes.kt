// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

interface Base {
    fun value(): String = "OK"
}

<!SUPERTYPE_APPEARS_TWICE!>@WithCompanion(Base::class, Base::class)
class Owner {
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, stringLiteral */
