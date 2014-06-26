package demo

import java.util.HashMap

fun Test(): Test {
    return Test()
}
fun Test(s: String): Test {
    return Test()
}

class Test

class User {
    fun main() {
        val m = HashMap(1)
        val m2 = HashMap(10)

        val t1 = Test()
        val t2 = Test("")
    }
}