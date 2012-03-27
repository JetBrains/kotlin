package kotlin.concurrent

abstract class FunctionalList<T>(public val size: Int) {
    public abstract val head: T
    public abstract val tail: FunctionalList<T>

    val empty : Boolean
        get() = size == 0

    fun add(element: T) : FunctionalList<T> = FunctionalList.Standard(element, this)

    fun reversed() : FunctionalList<T> {
        if(empty)
            return this

        var cur = tail
        var new = of(head)

        while(!cur.empty) {
            new = new.add(cur.head)
            cur = cur.tail
        }
        return new
    }

    fun iterator() : Iterator<T> = object: Iterator<T> {
        var cur = this@FunctionalList

        override fun next(): T {
            if(cur.empty)
                throw java.util.NoSuchElementException()

            val head = cur.head
            cur = cur.tail
            return head
        }

        override val hasNext: Boolean
        get() = !cur.empty
    }

    class object {
        class Empty<T>() : FunctionalList<T>(0) {
            override val head: T
            get() = throw java.util.NoSuchElementException()
            override val tail: FunctionalList<T>
            get() = throw java.util.NoSuchElementException()
        }

        class Standard<T>(override val head: T, override val tail: FunctionalList<T>) : FunctionalList<T>(tail.size+1)

        fun <T> emptyList() = Empty<T>()

        fun <T> of(element: T) : FunctionalList<T> = FunctionalList.Standard<T>(element,emptyList())
    }
}

