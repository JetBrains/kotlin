package kotlin

external fun kotlinclib_boolean_size(): Int

class BooleanArray(var size: Int) {
    val dataRawPtr: Int

    /** Returns the number of elements in the array. */
    //size: Int

    init {
        this.dataRawPtr = malloc_array(kotlinclib_boolean_size() * this.size)
        var index = 0
        while (index < this.size) {
            set(index, false)
            index = index + 1
        }
    }

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    operator fun get(index: Int): Boolean {
        val res = kotlinclib_get_byte(this.dataRawPtr, index) == 1.toByte()
        return res
    }


    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    operator fun set(index: Int, value: Boolean) {
        if (value == true) {
            kotlinclib_set_byte(this.dataRawPtr, index, 1.toByte())
        } else {
            kotlinclib_set_byte(this.dataRawPtr, index, 0.toByte())
        }
    }

    fun clone(): BooleanArray {
        val newInstance = BooleanArray(this.size)
        var index = 0
        while (index < this.size) {
            val value = this.get(index)
            newInstance.set(index, value)
            index = index + 1
        }

        return newInstance
    }
}

fun BooleanArray.print() {
    var index = 0
    print('[')
    while (index < size) {
        print(get(index))
        index++
        if (index < size){
            print(';')
            print(' ')
        }
    }
    print(']')
}

fun BooleanArray.println() {
    this.print()
    //println()
}


fun BooleanArray.copyOf(newSize: Int): BooleanArray {
    val newInstance = BooleanArray(newSize)
    var index = 0
    val end = if (newSize > this.size) this.size else newSize
    while (index < end) {
        val value = this.get(index)
        newInstance.set(index, value)
        index = index + 1
    }

    while (index < newSize) {
        newInstance.set(index, false)
        index = index + 1
    }

    return newInstance
}

fun BooleanArray.copyOfRange(fromIndex: Int, toIndex: Int): BooleanArray {
    val newInstance = BooleanArray(toIndex - fromIndex)
    var index = fromIndex
    while (index < toIndex) {
        val value = this.get(index)
        newInstance.set(index - fromIndex, value)
        index = index + 1
    }

    return newInstance
}

operator fun BooleanArray.plus(element: Boolean): BooleanArray {
    val index = size
    val result = this.copyOf(index + 1)
    result[index] = element
    return result
}

operator fun BooleanArray.plus(elements: BooleanArray): BooleanArray {
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