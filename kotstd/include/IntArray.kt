package kotlin

external fun kotlinclib_get_int(src: Int, index: Int): Int
external fun kotlinclib_set_int(src: Int, index: Int, value: Int)
external fun kotlinclib_int_size(): Int


class IntArray(var size: Int) {
    val dataRawPtr: Int

    /** Returns the number of elements in the array. */
    //size: Int

    init {
        this.dataRawPtr = malloc_array(kotlinclib_int_size() * this.size)
        var index = 0
        while (index < this.size) {
            set(index, 0)
            index = index + 1
        }
    }

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    operator fun get(index: Int): Int {
        return kotlinclib_get_int(this.dataRawPtr, index)
    }


    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    operator fun set(index: Int, value: Int) {
        kotlinclib_set_int(this.dataRawPtr, index, value)
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

fun IntArray.print() {
    var index = 0
    print('[')
    while (index < size) {
        print(get(index))
        index++
        if (index < size) {
            print(';')
            print(' ')
        }
    }
    print(']')
}

fun IntArray.println() {
    this.print()
    //println()
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

fun IntArray.max(from: Int = 0): Int {
    var result = from
    var i = from
    while (i < size - 1) {
        result = if (get(i) > get(result)) i else result
        i++
    }

    return get(result)
}

fun IntArray.min(from: Int = 0): Int {
    var result = from
    var i = from
    while (i < size - 1) {
        result = if (this.get(i) < this.get(result)) i else result
        i++
    }

    return this.get(result)
}

fun IntArray.sum(): Int {
    var result = 0
    var i = 0
    while (i < size - 1) {
        result += this.get(i)
        i++
    }

    return result
}

fun IntArray.sort(): IntArray {
    val result = this.clone()
    var i = 0
    while (i < size - 1) {
        result[i] = this.max(i)
        i++
    }

    return result
}

fun IntArray.mean(): Int =
        this.sum() / this.size

fun IntArray.median(): Int =
    this.sort()[this.size / 2]

fun IntArray.filter(predicate: (Int) -> Boolean): IntArray {
    var resultSize = 0
    var i = 0
    while (i < size - 1) {
        if (predicate(get(i))) {
            resultSize++
        }

        i++
    }

    val result = IntArray(resultSize)
    var j = 0
    i = 0
    while (i < size - 1) {
        if (predicate(get(i))) {
            result[j] = get(i)
            j++
        }

        i++
    }

    return result
}
