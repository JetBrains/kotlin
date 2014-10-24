// ERROR: 'foo' in 'A' is final and cannot be overridden
// ERROR: This type is final, so it cannot be inherited from
// ERROR: This type is final, so it cannot be inherited from
class A {
    fun foo() {
    }
}

class B : A() {
    override fun foo() {
    }
}

class C : B() {
    override fun foo() {
    }
}