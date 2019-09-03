package demo

import java.util.HashMap

internal class Test {
    constructor() {}
    constructor(s: String) {}
}

internal class User {
    fun main() {
        val m: HashMap<*, *> = HashMap<Any?, Any?>(1)
        val m2: HashMap<*, *> = HashMap<Any?, Any?>(10)
        val t1 = Test()
        val t2 = Test("")
    }
}