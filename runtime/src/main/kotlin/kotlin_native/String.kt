package kotlin_native

public interface Cloneable {
    public fun clone(): Any
}

class ByteArray(size: Int) : Cloneable {
    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_ByteArray_get")
    external public operator fun get(index: Int): Byte

    @SymbolName("Kotlin_ByteArray_set")
    external public operator fun set(index: Int, value: Byte): Unit

    @SymbolName("Kotlin_ByteArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_ByteArray_getArrayLength")
    external private fun getArrayLength(): Int
}

class CharArray(size: Int) : Cloneable {
    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_CharArray_get")
    external public operator fun get(index: Int): Char

    @SymbolName("Kotlin_CharArray_set")
    external public operator fun set(index: Int, value: Char): Unit

    @SymbolName("Kotlin_CharArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_CharArray_getArrayLength")
    external private fun getArrayLength(): Int
}

class IntArray(size: Int) : Cloneable {
    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_IntArray_get")
    external public operator fun get(index: Int): Char

    @SymbolName("Kotlin_IntArray_set")
    external public operator fun set(index: Int, value: Char): Unit

    @SymbolName("Kotlin_IntArray_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_IntArray_getArrayLength")
    external private fun getArrayLength(): Int
}

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