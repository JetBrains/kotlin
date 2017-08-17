package test

class Test<T : CharSequence, N : Number> {
    private val x = object : TypedListUpdateCallback<String, Long> {
        override fun onInserted(position: Long, count: Long, item: String) {}
    }
}

interface TypedListUpdateCallback<T : Any, C : Number> {
    fun onInserted(position: C, count: C, item: T)
}