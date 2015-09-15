package demo

internal class Collection<E> internal constructor(e: E) {
    init {
        println(e)
    }
}

internal class Test {
    internal fun main() {
        val raw1 = Collection(1)
        val raw2 = Collection(1)
        val raw3 = Collection("1")
    }
}