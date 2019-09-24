package demo

internal class Test {
    fun main() {
        val commonMap = HashMap<String, Int>()
        val rawMap: HashMap<*, *> = HashMap<String, Int>()
        val superRawMap: HashMap<*, *> = HashMap<Any?, Any?>()
    }
}