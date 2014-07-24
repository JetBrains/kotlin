package demo

class Collection<E>(e: E) {
    {
        System.out.println(e)
    }
}

class Test {
    fun main() {
        val raw1 = Collection(1)
        val raw2 = Collection<Int>(1)
        val raw3 = Collection<String>("1")
    }
}