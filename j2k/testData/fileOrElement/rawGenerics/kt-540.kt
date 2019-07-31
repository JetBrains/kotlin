// ERROR: Not enough information to infer type variable E
package demo

import java.util.ArrayList

internal class Test {
    fun main() {
        val common = ArrayList<String>()
        val raw = ArrayList<String>()
        val superRaw = ArrayList()
    }
}