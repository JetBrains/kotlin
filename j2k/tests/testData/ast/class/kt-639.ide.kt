package demo

import java.util.HashMap

class Test() {
    class object {
        fun init(): Test {
            val __ = Test()
            return __
        }
        fun init(s: String): Test {
            val __ = Test()
            return __
        }
    }
}
class User() {
    fun main() {
        val m = HashMap(1)
        val m2 = HashMap(10)
        val t1 = Test.init()
        val t2 = Test.init("")
    }
}