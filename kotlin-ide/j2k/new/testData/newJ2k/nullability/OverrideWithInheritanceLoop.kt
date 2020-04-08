// ERROR: There's a cycle in the inheritance hierarchy for this type
// ERROR: There's a cycle in the inheritance hierarchy for this type
internal open class A : B() {
    open fun foo(s: String?) {}
}

internal open class B : A() {
    open fun foo(s: String?) {}
}
