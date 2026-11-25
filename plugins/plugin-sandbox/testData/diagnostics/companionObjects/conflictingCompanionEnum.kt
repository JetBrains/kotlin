// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

interface Base

@WithCompanion(Base::class)
class <!COMPILER_PLUGIN_REDECLARATION!>Owner<!> {
    enum class <!REDECLARATION!>Companion<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, enumDeclaration, interfaceDeclaration, nestedClass */
