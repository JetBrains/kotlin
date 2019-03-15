// EXPECTED_REACHABLE_NODES: 1231
package foo

open class Base {
    val i: Int
    val i2: Int
    val i3: Int
    val bs: String

    constructor(s:String) {bs = s}

    fun foo() = bs

    init {
        i = 10
        i2 = 20
        i3 = 30
    }
}

class Test: Base {
    val t1: Int
    val t2: Int

    constructor(tt1: Int, tt2:Int) : super("OK") {
        t1 = tt1
        t2 = tt2
    }
}

fun box(): String {

    val t = Test(1, 2)
    return t.foo()
}