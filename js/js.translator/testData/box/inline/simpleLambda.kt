// EXPECTED_REACHABLE_NODES: 1284
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

// CHECK_BREAKS_COUNT: function=test1 count=0
// CHECK_LABELS_COUNT: function=test1 name=$l$block count=0
fun test1() : Int {
    val input = Z()
    return input.use<Z, Int>{
        100
    }
}

// CHECK_BREAKS_COUNT: function=test2 count=0
// CHECK_LABELS_COUNT: function=test2 name=$l$block count=0
fun test2() : Int {
    val x = 1000
    return use2() + x
}


fun box(): String {
    if (test1() != 100) return "test1: ${test1()}"
    if (test2() != 1100) return "test1: ${test2()}"

    return "OK"
}
