// ERROR: 'internal fun Test(): demo.Test' is already defined in demo
// ERROR: 'public constructor Test()' is already defined in demo
// ERROR: Overload resolution ambiguity:  internal fun Test(): demo.Test defined in demo public constructor Test() defined in demo.Test
// ERROR: Overload resolution ambiguity:  internal fun Test(): demo.Test defined in demo public constructor Test() defined in demo.Test
// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K, V>(p0: kotlin.Int) Please specify it explicitly.
// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K, V>(p0: kotlin.Int) Please specify it explicitly.
// ERROR: Overload resolution ambiguity:  internal fun Test(): demo.Test defined in demo public constructor Test() defined in demo.Test
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