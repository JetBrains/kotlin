// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

interface Left {
    fun value(): String = "Left"
}

interface Right {
    fun value(): String = "Right"
}

@WithCompanion(Left::class, Right::class)
class <!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>Owner<!> {
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, functionDeclaration, interfaceDeclaration,
objectDeclaration, stringLiteral */
