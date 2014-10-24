// ERROR: 'a' in 'A' is final and cannot be overridden
// ERROR: This type is final, so it cannot be inherited from
class A {
    fun a() {
    }
}

class B : A() {
    override fun a() {
    }
}