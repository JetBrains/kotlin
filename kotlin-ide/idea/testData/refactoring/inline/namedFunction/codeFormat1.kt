class A {
    fun foo() {
        ba<caret>r("", "")
    }

    fun bar(a: String, b: String) {
        baz(a)
        baz(b)
    }

    fun baz(a: String) {}
}