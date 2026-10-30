// ISSUE: KT-90508
// WITH_STDLIB
// FILE: usage.kt

fun usage(): User = User.builder().name("Alice").build()

fun usageBodyProperty() {
    User.builder().<!UNRESOLVED_REFERENCE!>greeting<!>("Hi")
}

// FILE: User.kt

import lombok.Builder

// The builder is built out of the primary constructor, so the implicitly typed body property is not a builder field
@Builder
class User(name: String) {
    var <!BUILDER_PROPERTY_NOT_INCLUDED!>greeting<!> = "Hello, $name"
}

// The body property shares its name with the parameter, so the builder field and setter come from the parameter
@Builder(toBuilder = true)
class SameName(greeting: String) {
    var greeting = greeting

    fun copy(): SameName = toBuilder().greeting("Hi").build()
}

@Builder(toBuilder = true)
class WithToBuilder(<!TO_BUILDER_CANNOT_OBTAIN!>name: String<!>) {
    var <!BUILDER_PROPERTY_NOT_INCLUDED!>greeting<!> = "Hello, $name"
}
