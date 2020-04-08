package test

class C : I {
    override suspend fun bar(s: String) { }
}

fun test(c: C) {
    c.bar("test")
}