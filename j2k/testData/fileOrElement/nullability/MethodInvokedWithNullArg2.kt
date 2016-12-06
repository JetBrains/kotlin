internal class C {
    fun foo(s: String?) {}
}

internal class D {
    fun bar(c: C) {
        c.foo(null)
    }
}