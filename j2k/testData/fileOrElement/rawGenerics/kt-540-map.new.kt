package demo

internal class Test {
    fun main() {
        val commonMap: HashMap<String?, Int?> = HashMap()
        val rawMap: HashMap<*, *> = HashMap<String?, Int?>()
        val superRawMap: HashMap<*, *> = HashMap<Any?, Any?>()
    }
}