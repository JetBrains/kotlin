object ArrayInitializerBugKt {
    private val GREETING = byteArrayOf(
        'H'.toByte(),
        'e'.toByte(),
        'l'.toByte(),
        'l'.toByte(),
        'o'.toByte(),
        ','.toByte(),
        ' '.toByte(),
        'b'.toByte(),
        'u'.toByte(),
        'g'.toByte(),
        '!'.toByte()
    )

    fun main(vararg args: String?) {
        val greeting = String(GREETING)
        println(greeting)
    }
}