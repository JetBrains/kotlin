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
