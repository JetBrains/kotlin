fun outer(a: Int) {
    fun local(a: Int) {
        if (a > 0) {
            outer(a - 1)
        }
    }
}