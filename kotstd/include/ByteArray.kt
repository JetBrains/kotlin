external fun malloc_array(size: Int): Int
external fun kotlinclib_get_byte(src: Int, index: Int): Byte
external fun kotlinclib_set_byte(src: Int, index: Int, value: Byte)
external fun kotlinclib_byte_size(): Int


class ByteArray(var size: Int) {
    val data: Int

    /** Returns the number of elements in the array. */
    //size: Int

    init {
        this.data = malloc_array(kotlinclib_byte_size() * this.size)
    }

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    operator fun get(index: Int): Byte {
        return kotlinclib_get_byte(this.data, index)
    }


    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    operator fun set(index: Int, value: Byte) {
        kotlinclib_set_byte(this.data, index, value)
    }


    fun clone(): ByteArray {
        val newInstance = ByteArray(this.size)
        var index = 0
        while (index < this.size) {
            val value = this.get(index)
            newInstance.set(index, value)
            index = index + 1
        }

        return newInstance
    }

}

fun ByteArray.copyOf(newSize: Int): ByteArray {
    val newInstance = ByteArray(newSize)
    var index = 0
    val end = if (newSize > this.size) this.size else newSize
    while (index < end) {
        val value = this.get(index)
        newInstance.set(index, value)
        index = index + 1
    }

    while (index < newSize) {
        newInstance.set(index, 0)
        index = index + 1
    }

    return newInstance
}

fun ByteArray.copyOfRange(fromIndex: Int, toIndex: Int): ByteArray {
    val newInstance = ByteArray(toIndex - fromIndex)
    var index = fromIndex
    while (index < toIndex) {
        val value = this.get(index)
        newInstance.set(index - fromIndex, value)
        index = index + 1
    }

    return newInstance
}

operator fun ByteArray.plus(element: Byte): ByteArray {
    val index = size
    val result = this.copyOf(index + 1)
    result[index] = element
    return result
}

operator fun ByteArray.plus(elements: ByteArray): ByteArray {
    val thisSize = size
    val arraySize = elements.size
    val resultSize = thisSize + arraySize
    val newInstance = this.copyOf(resultSize)
    var index = thisSize

    while (index < resultSize) {
        val value = elements.get(index - thisSize)
        newInstance.set(index, value)
        index = index + 1
    }

    return newInstance
}