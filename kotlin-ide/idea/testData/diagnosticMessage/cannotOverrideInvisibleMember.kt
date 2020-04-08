// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: CANNOT_OVERRIDE_INVISIBLE_MEMBER

open class A {
    private open fun foo() {}
}

class B : A() {
    override fun foo() {}
}
