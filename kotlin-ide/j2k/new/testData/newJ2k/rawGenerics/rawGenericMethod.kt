package demo

internal class TestT {
    fun <T> getT() {}
}

internal class U {
    fun main() {
        val t = TestT()
        t.getT<String>()
        t.getT<Int>()
        t.getT<Any>()
    }
}