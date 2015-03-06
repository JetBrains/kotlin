// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K, V>(p0: kotlin.Int) Please specify it explicitly.
// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K, V>(p0: kotlin.Int) Please specify it explicitly.
package demo

import java.util.HashMap

class Test {
    constructor() {
    }
    constructor(s: String) {
    }
}

class User {
    fun main() {
        val m = HashMap(1)
        val m2 = HashMap(10)

        val t1 = Test()
        val t2 = Test("")
    }
}