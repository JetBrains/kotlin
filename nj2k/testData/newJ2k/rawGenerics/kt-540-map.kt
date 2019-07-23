package demo

import java.util.HashMap

internal class Test {
    fun main() {
        val commonMap = HashMap<String, Int>()
        val rawMap: HashMap<*, *> = HashMap<String?, Int?>()
        val superRawMap: HashMap<*, *> = HashMap<Any?, Any?>()
    }
}