class JvmStaticTest {
    companion object {
        @JvmStatic
        val one = 1

        const val two = 2

        const val c: Char = 'C'
    }

    const val three: Byte = 3.toByte()
    const val d: Char = 'D'
}
