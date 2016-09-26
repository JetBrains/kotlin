package foo

class UseCurry {
    fun test() {
        val plus1 = Curry(Plus, 1)

        if (plus1(1) != 2) throw AssertionError()
    }

    private object Plus : Curry<Int, Int>.FN2 {
        override fun invoke(p0: Int, p1: Int): Int =
                p0 + p1
    }
}