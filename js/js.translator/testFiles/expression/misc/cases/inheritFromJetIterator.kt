package foo

class TabIterator : Iterator<Any?> {
    override val hasNext:Boolean
      get() = false

    override fun next():Any? {
        return null
    }
}

fun box() = !TabIterator().hasNext
