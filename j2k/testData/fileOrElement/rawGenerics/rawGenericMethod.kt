package demo

internal class TestT {
    internal fun <T> getT() {
    }
}

internal class U {
    internal fun main() {
        val t = TestT()
        t.getT<String>()
        t.getT<Int>()
        t.getT<Any>()
    }
}