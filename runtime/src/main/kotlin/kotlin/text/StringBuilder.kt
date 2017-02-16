package kotlin.text

@SymbolName("Kotlin_String_fromUtf8Array")
external fun fromUtf8Array(array: ByteArray, start: Int, size: Int) : String

// TODO: make it somewhat private?
@SymbolName("Kotlin_String_fromCharArray")
external fun fromCharArray(array: CharArray, start: Int, size: Int) : String

@SymbolName("Kotlin_String_toCharArray")
external fun toCharArray(string: String) : CharArray


class StringBuilder private constructor (
        private var array: CharArray
) : CharSequence, Appendable {
    constructor() : this(10)

    constructor(capacity: Int) : this(CharArray(capacity))

    constructor(string: String) : this(toCharArray(string)) {
        length = array.size
    }

    override var length: Int = 0
        set(capacity) {
            ensureCapacity(capacity)
            field = capacity
        }

    override fun get(index: Int): Char {
        checkIndex(index)
        return array[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = substring(startIndex, endIndex)

    override fun toString(): String = fromCharArray(array, 0, length)

    fun substring(startIndex: Int, endIndex: Int): String {
        checkInsertIndex(startIndex)
        checkInsertIndexFrom(endIndex, startIndex)
        return fromCharArray(array, startIndex, endIndex - startIndex)
    }

    fun trimToSize() {
        if (length < array.size)
            array = array.copyOf(length)
    }

    fun ensureCapacity(capacity: Int) {
        if (capacity > array.size) {
            var newSize = array.size * 3 / 2
            if (capacity > newSize)
                newSize = capacity
            array = array.copyOf(newSize)
        }
    }

    // Of Appenable.
    override fun append(c: Char) : Appendable {
        ensureExtraCapacity(1)
        array[length++] = c
        return this
    }

    override fun append(csq: CharSequence?): Appendable {
        // TODO: how to treat nulls properly?
        if (csq == null) return this
        return append(csq, 0, csq.length)
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): Appendable {
        // TODO: how to treat nulls properly?
        if (csq == null) return this
        ensureExtraCapacity(end - start)
        var index = start
        while (index < end)
            array[length++] = csq[index++]
        return this
    }

    fun append(it: CharArray) {
        ensureExtraCapacity(it.size)
        for (c in it)
            array[length++] = c
    }

    fun append(it: String) {
        ensureExtraCapacity(it.length)
        for (c in toCharArray(it))
            array[length++] = c
    }

    fun append(it: Boolean) = append(it.toString())
    fun append(it: Byte) = append(it.toString())
    fun append(it: Short) = append(it.toString())
    fun append(it: Int) = append(it.toString())
    fun append(it: Long) = append(it.toString())
    fun append(it: Float) = append(it.toString())
    fun append(it: Double) = append(it.toString())
    fun append(it: Any?) = append(it.toString())

    // ---------------------------- private ----------------------------

    private fun ensureExtraCapacity(n: Int) {
        ensureCapacity(length + n)
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= length) throw IndexOutOfBoundsException()
    }

    private fun checkInsertIndex(index: Int) {
        if (index < 0 || index > length) throw IndexOutOfBoundsException()
    }

    private fun checkInsertIndexFrom(index: Int, fromIndex: Int) {
        if (index < fromIndex || index > length) throw IndexOutOfBoundsException()
    }
}