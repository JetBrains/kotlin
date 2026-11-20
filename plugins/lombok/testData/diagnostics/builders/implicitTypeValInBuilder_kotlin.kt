// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-90508
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
// FILE: usage.kt

fun usage(): Entity = Entity.builder().title("Main").build()

fun usageImplicitVar() {
    Entity.builder().<!UNRESOLVED_REFERENCE!>counter<!>(1)
}

fun usageVals() {
    Entity.builder().<!UNRESOLVED_REFERENCE!>id<!>(42)
    Entity.builder().<!UNRESOLVED_REFERENCE!>tags<!>(listOf("a"))
    Entity.builder().<!UNRESOLVED_REFERENCE!>explicit<!>("a")
}

fun usageWithConstructor(): WithConstructor = WithConstructor.builder()
    .name("Bob")
    .<!UNRESOLVED_REFERENCE!>counter<!>(1)
    .build()

// FILE: Entity.kt

import lombok.Builder

// A read-only property is never a builder field, so its implicit type is not reported
@Builder
class Entity {
    val <!BUILDER_PROPERTY_NOT_INCLUDED!>id<!> = 42
    val <!BUILDER_PROPERTY_NOT_INCLUDED!>tags<!> = emptyList<String>()
    val <!BUILDER_PROPERTY_NOT_INCLUDED!>explicit<!>: String = ""
    var title: String = ""
    var <!BUILDER_PROPERTY_NOT_INCLUDED_ERROR!>counter<!> = 0
}

// The builder is built out of the primary constructor, so body properties are not builder fields at all
@Builder
class WithConstructor(val name: String) {
    val <!BUILDER_PROPERTY_NOT_INCLUDED!>id<!> = 42
    var <!BUILDER_PROPERTY_NOT_INCLUDED!>counter<!> = 0
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, primaryConstructor, propertyDeclaration,
stringLiteral */
