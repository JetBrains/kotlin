// EXPECTED_REACHABLE_NODES: 1287
package foo

class Test(val bs: String) {

    var i: Int = 0
    var i2: Int = 0
    var i3: Int =0

    constructor(ii1: Int, ii2: Int): this("OK") {
        i = ii1
        i2 = ii2
        i3 = 30
    }

    constructor(ii: Int): this(ii, 18) {
    }

    fun foo() = bs
}

fun box(): String {

    val t = Test(1)
    return t.foo()
}