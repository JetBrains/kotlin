package demo

import java.util.HashMap

class Test private() {
    class object {
        fun create(): Test {
            return Test()
        }
        fun create(s: String): Test {
            return Test()
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