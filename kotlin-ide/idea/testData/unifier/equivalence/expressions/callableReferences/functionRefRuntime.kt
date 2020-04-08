class A {
    fun bar() {

    }

    fun baz() {

    }

    fun foo() {
        <selection>A::bar</selection>
        A::baz
        A::bar
        A::baz
    }
}