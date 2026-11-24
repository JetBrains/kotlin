// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

object Base {
}

<!SINGLETON_IN_SUPERTYPE!>@WithCompanion(Base::class)
class Owner {
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, classReference, objectDeclaration */
