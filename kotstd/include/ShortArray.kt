external fun kotlinclib_get_short(src: Int, index: Int): Short
external fun kotlinclib_set_short(src: Int, index: Int, value: Short)
external fun kotlinclib_short_size(): Int


class ShortArray(var size: Int) {
    val data: Int

    /** Returns the number of elements in the array. */
    //size: Int

    init {
        this.data = malloc_array(kotlinclib_short_size() * this.size)
    }

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    operator fun get(index: Int): Short {
        return kotlinclib_get_short(this.data, index)
    }


    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    operator fun set(index: Int, value: Short) {
        kotlinclib_set_short(this.data, index, value)
    }


    fun clone(): ShortArray {
        val newInstance = ShortArray(this.size)
        var index = 0
        while (index < this.size) {
            val value = this.get(index)
            newInstance.set(index, value)
            index = index + 1
        }

        return newInstance
    }

}
