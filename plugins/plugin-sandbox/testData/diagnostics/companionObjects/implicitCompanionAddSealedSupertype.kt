// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

sealed interface Base {
    object Known : Base
}

@WithCompanion(Base::class)
class Owner {
}

fun describe(base: Base): String = when (base) { // Should report NO_ELSE_IN_WHEN.
    Base.Known -> "Known"
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, equalityExpression, functionDeclaration,
interfaceDeclaration, nestedClass, objectDeclaration, sealed, stringLiteral, whenExpression, whenWithSubject */
