// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

open class Base {
    fun value(): String = "OK"
}

<!MANY_CLASSES_IN_SUPERTYPE_LIST, SUPERTYPE_APPEARS_TWICE!>@WithCompanion(Base::class, Base::class)
class Owner {
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, stringLiteral */
