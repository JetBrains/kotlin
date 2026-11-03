// ISSUE: KT-90508
// WITH_STDLIB
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
    val id = 42
    val tags = emptyList<String>()
    val explicit: String = ""
    var title: String = ""
    var <!BUILDER_PROPERTY_WITH_IMPLICIT_TYPE!>counter<!> = 0
}

// The builder is built out of the primary constructor, so body properties are not builder fields at all
@Builder
class WithConstructor(val name: String) {
    val id = 42
    var counter = 0
}
