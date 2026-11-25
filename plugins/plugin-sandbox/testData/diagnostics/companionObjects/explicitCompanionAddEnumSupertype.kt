// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

enum class Base {
    Known
}

@WithCompanion(Base::class)
class Owner {
    companion <!FINAL_SUPERTYPE!>object<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, equalityExpression, functionDeclaration,
interfaceDeclaration, nestedClass, objectDeclaration, sealed, stringLiteral, whenExpression, whenWithSubject */
