package kotlin

// TODO: remove that, as RTTI shall be per instantiation.
@ExportTypeInfo("theArrayTypeInfo")
public final class Array<T> : Cloneable {
    // Constructors are handled with compiler magic.
    public constructor(size: Int, init: (Int) -> T) {
        var index = 0
        while (index < size) {
            this[index] = init(index)
            index++
        }
    }
    internal constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_Array_clone")
    external public override fun clone(): Any

    @SymbolName("Kotlin_Array_get")
    external public operator fun get(index: Int): T

    @SymbolName("Kotlin_Array_set")
    external public operator fun set(index: Int, value: T): Unit

    public operator fun iterator(): kotlin.collections.Iterator<T> {
        return IteratorImpl(this)
    }

    // Konan-specific.
    @SymbolName("Kotlin_Array_getArrayLength")
    external private fun getArrayLength(): Int
}

private class IteratorImpl<T>(val collection: Array<T>) : Iterator<T> {
    var index : Int = 0

    public override fun next(): T {
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

public fun <T, C : MutableCollection<in T>> Array<out T>.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

@kotlin.internal.InlineOnly
public inline operator fun <T> Array<T>.plus(elements: Array<T>): Array<T> {
    val result = copyOfUninitializedElements(this.size + elements.size)
    elements.copyRangeTo(result, 0, elements.size, this.size)
    return result
}
