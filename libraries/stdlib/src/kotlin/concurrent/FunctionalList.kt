package kotlin.concurrent

public abstract class FunctionalList<T>(public val size: Int) {
    public abstract val head: T
    public abstract val tail: FunctionalList<T>

    public val empty: Boolean
        get() = size == 0

    public fun add(element: T): FunctionalList<T> = FunctionalList.Standard(element, this)

    public fun reversed(): FunctionalList<T> {
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

    public fun iterator() : Iterator<T> = object: Iterator<T> {
        var cur = this@FunctionalList

        public override fun next(): T {
            if(cur.empty)
                throw java.util.NoSuchElementException()

            val head = cur.head
            cur = cur.tail
            return head
        }

        override fun hasNext(): Boolean = !cur.empty
    }

    private class Empty<T>() : FunctionalList<T>(0) {
        override val head: T
            get() = throw java.util.NoSuchElementException()
        override val tail: FunctionalList<T>
            get() = throw java.util.NoSuchElementException()
    }

    private class Standard<T>(override val head: T, override val tail: FunctionalList<T>) : FunctionalList<T>(tail.size + 1)

    class object {
        public fun <T> emptyList(): FunctionalList<T> = Empty<T>()

        public fun <T> of(element: T): FunctionalList<T> = FunctionalList.Standard<T>(element, emptyList())
    }
}

