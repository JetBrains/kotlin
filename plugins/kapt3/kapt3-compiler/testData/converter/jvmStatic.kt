class JvmStaticTest {
    companion object {
        @JvmStatic
        val one = 1

        val two = 2

        const val c: Char = 'C'
    }

    val three: Byte = 3.toByte()
    val d: Char = 'D'
}
