// EXPECTED_REACHABLE_NODES: 1290
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/simple/params.1.kt
 */

package foo

class Inline() {

    inline fun foo1Int(s : (l: Int) -> Int, param: Int) : Int {
        return s(param)
    }

    inline fun foo1Double(param: Double, s : (l: Double) -> Double) : Double {
        return s(param)
    }

    inline fun foo2Param(param1: Double, s : (i: Int, l: Double) -> Double, param2: Int) : Double {
        return s(param2, param1)
    }
}

// CHECK_BREAKS_COUNT: function=test1 count=0
// CHECK_LABELS_COUNT: function=test1 name=$l$block count=0
fun test1(): Int {
    val inlineX = Inline()
    return inlineX.foo1Int({ z: Int -> z}, 25)
}

// CHECK_BREAKS_COUNT: function=test2 count=0
// CHECK_LABELS_COUNT: function=test2 name=$l$block count=0
fun test2(): Double {
    val inlineX = Inline()
    return inlineX.foo1Double(25.0, { z: Double -> z})
}

// CHECK_BREAKS_COUNT: function=test3 count=0
// CHECK_LABELS_COUNT: function=test3 name=$l$block count=0
fun test3(): Double {
    val inlineX = Inline()
    return inlineX.foo2Param(15.0, { z1: Int, z2: Double -> z1 + z2}, 10)
}

// CHECK_BREAKS_COUNT: function=test3WithCaptured count=0
// CHECK_LABELS_COUNT: function=test3WithCaptured name=$l$block count=0
fun test3WithCaptured(): Double {
    val inlineX = Inline()
    var c = 11.0;
    return inlineX.foo2Param(15.0, { z1: Int, z2: Double -> z1 + z2 + c}, 10)
}


fun box(): String {
    if (test1() != 25) return "test1: ${test1()}"
    if (test2() != 25.0) return "test2: ${test2()}"
    if (test3() != 25.0) return "test3: ${test3()}"
    if (test3WithCaptured() != 36.0) return "test3WithCaptured: ${test3WithCaptured()}"


    return "OK"
}