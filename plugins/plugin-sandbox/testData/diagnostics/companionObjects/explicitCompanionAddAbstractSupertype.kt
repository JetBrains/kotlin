// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

abstract class Base {
    fun value(): String = "OK"
}

@WithCompanion(Base::class)
class Owner { // Companion is not generated correctly for additional super classes.
    companion object
}

fun test() {
    val _ = Owner.value()
    val _ = Owner.Companion.value()
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, functionDeclaration, localProperty,
objectDeclaration, propertyDeclaration, stringLiteral, unnamedLocalVariable */
