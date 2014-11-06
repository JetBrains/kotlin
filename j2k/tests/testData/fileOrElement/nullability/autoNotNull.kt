class A {
    fun foo(s: String?): Int {
        if (s != null) {
            return s.length()
        }
        return -1
    }
}