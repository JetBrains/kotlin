package demo

import java.util.HashMap

class Test private() {
    class object {
        fun create(): Test {
            val __ = Test()
            return __
        }
        fun create(s: String): Test {
            val __ = Test()
            return __
        }
    }
}

class User {
    fun main() {
        val m = HashMap(1)
        val m2 = HashMap(10)

        val t1 = Test.create()
        val t2 = Test.create("")
    }
}