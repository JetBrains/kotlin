package demo

internal class Collection<E>(e: E) {
    init {
        println(e)
    }
}

internal class Test {
    fun main() {
        val raw1: Collection<*> = Collection<Any?>(1)
        val raw2: Collection<*> = Collection(1)
        val raw3: Collection<*> = Collection("1")
    }
}