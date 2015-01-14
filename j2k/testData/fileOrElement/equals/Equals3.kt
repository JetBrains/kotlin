class C {
    public fun equals(c: C): Boolean {
        return false
    }

    fun foo(c1: C, c2: C): Boolean {
        return c1.equals(c2)
    }
}