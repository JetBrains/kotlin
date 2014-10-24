// ERROR: There's a cycle in the inheritance hierarchy for this type
// ERROR: There's a cycle in the inheritance hierarchy for this type
// ERROR: This type is final, so it cannot be inherited from
// ERROR: This type is final, so it cannot be inherited from
class A : B() {
    public fun foo(s: String) {
    }
}

class B : A() {
    public fun foo(s: String) {
    }
}
