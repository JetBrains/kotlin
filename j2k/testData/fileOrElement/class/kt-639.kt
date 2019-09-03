// ERROR: Not enough information to infer type variable K
// ERROR: Not enough information to infer type variable K
package demo

import java.util.HashMap

internal class Test {
    constructor() {}
    constructor(s: String) {}
}

internal class User {
    fun main() {
        val m = HashMap(1)
        val m2 = HashMap(10)

        val t1 = Test()
        val t2 = Test("")
    }
}
