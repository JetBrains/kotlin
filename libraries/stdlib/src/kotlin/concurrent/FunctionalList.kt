package kotlin.concurrent

abstract class FunctionalList<T>(public val size: Int) {
    public abstract val head: T
    public abstract val tail: FunctionalList<T>

    val empty : Boolean
        get() = size == 0

    public fun add(element: T) : FunctionalList<T> = FunctionalList.Standard(element, this)

    public fun reversed() : FunctionalList<T> {
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

        class Standard<T>(public override val head: T, public override val tail: FunctionalList<T>) : FunctionalList<T>(tail.size+1)

        public fun <T> emptyList() : FunctionalList<T> = Empty<T>()

        public fun <T> of(element: T) : FunctionalList<T> = FunctionalList.Standard<T>(element,emptyList())
    }
}

