package foo

class TabIterator : Iterator<Any?> {
    override fun hasNext(): Boolean = false

    override fun next(): Any? {
        return null
    }
}

fun box() = !TabIterator().hasNext()
