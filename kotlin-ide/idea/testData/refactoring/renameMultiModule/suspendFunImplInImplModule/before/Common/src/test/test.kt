package test

interface I {
    suspend fun foo(s: String)
}

fun test(i: I) {
    i.foo("test")
}