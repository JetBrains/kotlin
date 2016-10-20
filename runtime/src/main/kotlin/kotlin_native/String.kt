package kotlin_native

class ByteArray(size: Int) {
    public val size: Int

    external public operator fun get(index: Int): Byte

    external public operator fun set(index: Int, value: Byte): Unit
}

class String {
    companion object {
        external fun fromUtf8Array(ByteArray: array) : String
    }
    external public operator fun plus(other: Any?): String

    public val length: Int
        get() = getStringLength()

    // Can be O(N).
    external public fun get(index: Int): Char

    // external public fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    external public fun compareTo(other: String): Int

    external private fun getStringLength(): Int
}