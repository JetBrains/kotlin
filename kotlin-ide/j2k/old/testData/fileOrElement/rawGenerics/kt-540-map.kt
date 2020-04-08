// ERROR: Not enough information to infer type variable K
package demo

import java.util.HashMap

internal class Test {
    fun main() {
        val commonMap = HashMap<String, Int>()
        val rawMap = HashMap<String, Int>()
        val superRawMap = HashMap()
    }
}