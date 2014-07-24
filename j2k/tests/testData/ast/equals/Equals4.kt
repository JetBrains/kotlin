class C {
    fun foo(s1: String, s2: String, s3: String, s4: String): Boolean {
        return s1 == s2 == (s3 != s4)
    }
}