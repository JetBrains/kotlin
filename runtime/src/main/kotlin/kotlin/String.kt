package kotlin

@SymbolName("Kotlin_String_fromUtf8Array")
external fun fromUtf8Array(array: ByteArray) : String

// TODO: enable, once global variables implemented.
// @SymbolName("theEmptyString")
// val theEmptyString = ""

@ExportTypeInfo("theStringTypeInfo")
public final class String : Comparable<String>, CharSequence {
    @SymbolName("Kotlin_String_hashCode")
    external public override fun hashCode(): Int

    // TODO: make it Any?
    public operator fun plus(other: Any): String {
        return plusImpl(other.toString())
    }

    override public fun toString(): String {
        return this
    }

    public override val length: Int
        get() = getStringLength()

    // Can be O(N).
    @SymbolName("Kotlin_String_get")
    external override public fun get(index: Int): Char

    @SymbolName("Kotlin_String_subSequence")
    external override public fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    @SymbolName("Kotlin_String_compareTo")
    override external public fun compareTo(other: String): Int

    @SymbolName("Kotlin_String_getStringLength")
    external private fun getStringLength(): Int

    @SymbolName("Kotlin_String_plusImpl")
    external private fun plusImpl(other: Any): String

    @SymbolName("Kotlin_String_equals")
    external public override fun equals(other: Any?): Boolean
}

// TODO: in big Kotlin this operations are in kotlin.kotlin_builtins.
private val kNullString = "<null>"
public operator fun kotlin.String?.plus(other: kotlin.Any?): kotlin.String =
    this?.plus(other?.toString() ?: kNullString) ?: other?.toString() ?: kNullString


public fun Any?.toString() = this?.toString() ?: kNullString