// EXPECTED_REACHABLE_NODES: 1230
package foo

open class Base {
    val i: Int
    val i2: Int
    val i3: Int
    val bs: String

    constructor(ii1: Int, ii2: Int) {
        i = ii1
        i2 = ii2
        i3 = 30
    }

    constructor(ii: Int):  this(ii, 25) {
    }

    open fun foo() = bs

    init {
        bs = "fail"
    }
}

class Test(val tt: String) : Base(18) {

    override fun foo() = tt
}

fun box(): String {

    val t = Test("OK")
    return t.foo()
}