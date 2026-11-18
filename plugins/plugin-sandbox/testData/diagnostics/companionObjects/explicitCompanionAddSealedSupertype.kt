// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

sealed interface Base {
    object Known : Base
}

@WithCompanion(Base::class)
class Owner {
    companion object
}

fun describe(base: Base): String = <!NO_ELSE_IN_WHEN!>when<!> (base) {
    Base.Known -> "Known"
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, equalityExpression, functionDeclaration,
interfaceDeclaration, nestedClass, objectDeclaration, sealed, stringLiteral, whenExpression, whenWithSubject */
