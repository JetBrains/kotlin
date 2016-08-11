external fun kotlinclib_get_int(src: Int, index: Int): Int
external fun kotlinclib_set_int(src: Int, index: Int, value: Int)


class IntArray(var size: Int) {
    val data: Int

    /** Returns the number of elements in the array. */
    //size: Int

    init {
        this.data = malloc_array(4 * this.size)
    }

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    operator fun get(index: Int): Int {
        return kotlinclib_get_int(this.data, index)
    }


    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    operator fun set(index: Int, value: Int) {
        kotlinclib_set_int(this.data, index, value)
    }


    fun clone(): IntArray {
        val newInstance = IntArray(this.size)
        var index = 0
        while (index < this.size) {
            val value = this.get(index)
            newInstance.set(index, value)
            index = index + 1
        }

        return newInstance
    }

}

fun IntArray.copyOf(newSize: Int): IntArray {
    val newInstance = IntArray(newSize)
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

fun IntArray.copyOfRange(fromIndex: Int, toIndex: Int): IntArray {
    val newInstance = IntArray(toIndex - fromIndex)
    var index = fromIndex
    while (index < toIndex) {
        val value = this.get(index)
        newInstance.set(index - fromIndex, value)
        index = index + 1
    }

    return newInstance
}

operator fun IntArray.plus(element: Int): IntArray {
    val index = size
    val result = this.copyOf(index + 1)
    result[index] = element
    return result
}

operator fun IntArray.plus(elements: IntArray): IntArray {
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