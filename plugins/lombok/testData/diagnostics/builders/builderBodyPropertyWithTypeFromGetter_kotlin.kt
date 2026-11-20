// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import lombok.Builder
import lombok.Singular

// The type of a body property may come from its getter or initializer only: no builder field can be generated before it is resolved.
@Builder
class TypeFromGetter {
    var <!BUILDER_PROPERTY_NOT_INCLUDED_ERROR!>fromGetter<!>
        get() = 42
        set(value) {}

    var <!BUILDER_PROPERTY_NOT_INCLUDED_ERROR!>fromInitializer<!> = "x"

    @Singular("item") var <!BUILDER_PROPERTY_NOT_INCLUDED_ERROR!>items<!> = listOf("a")
}

fun test() {
    TypeFromGetter.builder().<!UNRESOLVED_REFERENCE!>fromGetter<!>(1).fromInitializer("y").item("b").build()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, propertyDeclaration, setter,
stringLiteral */
