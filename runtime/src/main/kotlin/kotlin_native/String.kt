package kotlin_native

@SymbolName("Kotlin_String_fromUtf8Array")
external fun fromUtf8Array(array: ByteArray) : String

class String {
    public operator fun plus(other: Any?): String {
        return plusImpl(other.toString())
    }

    public val length: Int
        get() = getStringLength()

    // Can be O(N).
    @SymbolName("Kotlin_String_get")
    external public fun get(index: Int): Char

    // external public fun subSequence(startIndex: Int, endIndex: Int): CharSequence
    @SymbolName("Kotlin_String_compareTo")
    external public fun compareTo(other: String): Int

    @SymbolName("Kotlin_String_getStringLength")
    external private fun getStringLength(): Int

    @SymbolName("Kotlin_String_plusImpl")
    external private fun plusImpl(other:Any): String
}