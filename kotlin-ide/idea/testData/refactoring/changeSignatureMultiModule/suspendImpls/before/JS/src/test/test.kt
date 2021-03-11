package test

class C : I {
    suspend override fun foo(s: String) = s.length
}