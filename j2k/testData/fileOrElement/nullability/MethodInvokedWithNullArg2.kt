internal class C {
    fun foo(s: String?) {
    }
}

internal class D {
    internal fun bar(c: C) {
        c.foo(null)
    }
}