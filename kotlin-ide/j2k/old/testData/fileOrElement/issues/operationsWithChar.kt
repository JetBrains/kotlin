internal class Test {
    fun operationsWithChar() {
        val c: Char = 1.toChar()
        val i = 1

        b(i > c.toInt())
        b(i >= c.toInt())
        b(i < c.toInt())
        b(i <= c.toInt())

        b(c.toInt() > i)
        b(c.toInt() >= i)
        b(c.toInt() < i)
        b(c.toInt() <= i)

        b(c.toInt() == i)
        b(c.toInt() != i)
        b(i == c.toInt())
        b(i != c.toInt())

        i(i + c.toInt())
        i(i - c.toInt())
        i(i / c.toInt())
        i(i * c.toInt())
        i(i % c.toInt())
        i(i or c.toInt())
        i(i and c.toInt())
        i(i shl c.toInt())
        i(i shr c.toInt())

        i(c.toInt() + i)
        i(c.toInt() - i)
        i(c.toInt() / i)
        i(c.toInt() * i)
        i(c.toInt() % i)
        i(c.toInt() or i)
        i(c.toInt() and i)
        i(c.toInt() shl i)
        i(c.toInt() shr i)
    }

    fun b(b: Boolean) {}
    fun i(i: Int) {}
}