// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K : Any!, V : Any!>(initialCapacity: Int) Please specify it explicitly.
// ERROR: Type inference failed: Not enough information to infer parameter K in constructor HashMap<K : Any!, V : Any!>(initialCapacity: Int) Please specify it explicitly.
package demo

internal class Test {
    constructor() {}
    constructor(s: String?) {}
}

internal class User {
    fun main() {
        val m: HashMap<*, *> = HashMap<Any?, Any?>(1)
        val m2: HashMap<*, *> = HashMap<Any?, Any?>(10)

        val t1 = Test()
        val t2 = Test("")
    }
}
