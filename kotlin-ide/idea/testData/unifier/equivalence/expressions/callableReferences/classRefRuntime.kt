class A {
    inner class B

    inner class C

    fun foo() {
        <selection>A::B</selection>
        A::C
        A::B
        A::C
    }
}