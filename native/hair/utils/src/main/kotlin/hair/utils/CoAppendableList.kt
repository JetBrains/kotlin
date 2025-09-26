package hair.utils

class CoAppendableList<T> private constructor(private val elements: MutableList<T>) : Collection<T>  by elements {
    constructor() : this(mutableListOf())

    fun add(element: T) {
        elements.add(element)
    }

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        var index = 0
        override fun hasNext(): Boolean = index < elements.size
        override fun next(): T = elements[index++]
    }
}