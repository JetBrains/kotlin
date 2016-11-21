package kotlin

// TODO: remove that, as RTTI shall be per instantiation.
@ExportTypeInfo("theArrayTypeInfo")
public final class Array<T> : Cloneable {
    // TODO: actual constructor has initializer parameter, implement it once lambdas are implemented.
    // Constructors are handled with compiler magic.
    public constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

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