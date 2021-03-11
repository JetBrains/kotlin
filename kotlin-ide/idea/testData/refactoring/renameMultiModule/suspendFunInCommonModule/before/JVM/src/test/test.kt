package test

class C : I {
    override suspend fun foo(s: String) { }
}

fun test(c: C) {
    c.foo("test")
}