// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K : kotlin.Any!, V : kotlin.Any!>(p0: kotlin.Int) Please specify it explicitly.
// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K : kotlin.Any!, V : kotlin.Any!>(p0: kotlin.Int) Please specify it explicitly.
package demo

import java.util.HashMap

internal class Test {
    internal constructor() {
    }

    internal constructor(s: String) {
    }
}

internal class User {
    internal fun main() {
        val m = HashMap(1)
        val m2 = HashMap(10)

        val t1 = Test()
        val t2 = Test("")
    }
}