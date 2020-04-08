package demo

internal object Test {
    fun bar(a: Int) {
        if (a < 0)
            throw RuntimeException("a = $a")
    }
}