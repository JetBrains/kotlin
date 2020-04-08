interface Callable<T> {
    fun call(): T
}

fun foo(a: Int): Int {
    // SIBLING:
    val o = object: Callable<Int> {
        val b: Int = 1

        override fun call(): Int {
            return <selection>a + b</selection>
        }
    }
    return o.call()
}