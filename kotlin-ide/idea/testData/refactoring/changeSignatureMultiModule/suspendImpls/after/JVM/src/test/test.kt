package test

class D : I {
    suspend override fun foo(s: String, n: Int) = s.length
}