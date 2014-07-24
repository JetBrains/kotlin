package demo

class TestT {
    fun <T> getT() {
    }
}

class U {
    fun main() {
        val t = TestT()
        t.getT<String>()
        t.getT<Int>()
        t.getT()
    }
}