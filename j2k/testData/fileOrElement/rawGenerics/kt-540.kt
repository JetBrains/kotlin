// ERROR: Type inference failed: Not enough information to infer parameter E in constructor ArrayList<E : Any!>() Please specify it explicitly.
package demo

import java.util.ArrayList

internal class Test {
    fun main() {
        val common = ArrayList<String>()
        val raw = ArrayList<String>()
        val superRaw = ArrayList()
    }
}