// EXPECTED_REACHABLE_NODES: 491
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/simple/simpleLambda.1.kt
 */

package foo

public class Data()

public inline fun <T, R> T.use(block: (T)-> R) : R {
    return block(this)
}

public inline fun use2() : Int {
    val s = 100
    return s
}

class Z {}

fun test1() : Int {
    val input = Z()
    return input.use<Z, Int>{
        100
    }
}

fun test2() : Int {
    val x = 1000
    return use2() + x
}


fun box(): String {
    if (test1() != 100) return "test1: ${test1()}"
    if (test2() != 1100) return "test1: ${test2()}"

    return "OK"
}
