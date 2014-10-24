// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K, V>() Please specify it explicitly.
package demo

import java.util.HashMap

class Test {
    fun main() {
        val commonMap = HashMap<String, Int>()
        val rawMap = HashMap<String, Int>()
        val superRawMap = HashMap()
    }
}