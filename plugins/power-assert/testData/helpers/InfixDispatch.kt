package infix.dispatch

class Wrapper<V>(
  private val value: V,
) {
    infix fun mustEqual(expected: V): Unit = assert(value == expected)

    fun mustEqual(expected: V, message: () -> String): Unit =
        assert(value == expected, message)

    override fun toString() = "Wrapper"
}
