// ERROR: There's a cycle in the inheritance hierarchy for this type
// ERROR: There's a cycle in the inheritance hierarchy for this type
open class A : B() {
    public open fun foo(s: String) {
    }
}

open class B : A() {
    public open fun foo(s: String) {
    }
}
