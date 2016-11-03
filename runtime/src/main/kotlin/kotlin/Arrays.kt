package kotlin

@ExportTypeInfo("theArrayTypeInfo")
class Array<T> : Cloneable {
    // Constructors are handled with compiler magic.
    private constructor() {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_Array_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_Array_get")
    external public operator fun get(index: Int): T

    @SymbolName("Kotlin_Array_set")
    external public operator fun set(index: Int, value: T): Unit

    @SymbolName("Kotlin_Array_getArrayLength")
    external private fun getArrayLength(): Int
}

@ExportTypeInfo("theByteArrayTypeInfo")
class ByteArray : Cloneable {
    // Constructors are handled with compiler magic.
    private constructor() {}

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

@ExportTypeInfo("theCharArrayTypeInfo")
class CharArray : Cloneable {
    // Constructors are handled with the compiler magic.
    private constructor() {}

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

@ExportTypeInfo("theIntArrayTypeInfo")
class IntArray : Cloneable {
    // Constructors are handled with the compiler magic.
    private constructor() {}

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