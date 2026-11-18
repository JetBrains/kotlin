// MODULE: main
// FILE: sample/ErroneousDeclarations.kt
package sample

typealias Text = String

// CONFLICTING_OVERLOADS: the signatures clash after alias expansion, but the ID keeps the alias as
// written in the source, so the two declarations stay distinguishable.
fun clashing(value: String) {}
fun clashing(value: Text) {}

// UNRESOLVED_REFERENCE: the parameter type is an error type; the ID still contains it.
fun unresolvedParameter(value: Undefined) {}

// UNRESOLVED_REFERENCE on the supertype.
class BrokenSupertype : UndefinedInterface

// UNRESOLVED_REFERENCE in the type parameter bound.
fun <T : MissingBound> unresolvedBound(value: T) {}

// Errors inside a member: the ID of the member is annotated, not the ID of the class.
class Container {
    fun brokenMember(value: AlsoUndefined): AlsoUndefined = value
}
