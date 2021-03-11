class F {
    fun f(a: Int, other: F) {
        if (a > 0) {
            other.f(a - 1)
        }
    }
}
