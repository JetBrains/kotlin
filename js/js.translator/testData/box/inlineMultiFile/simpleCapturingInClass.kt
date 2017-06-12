// EXPECTED_REACHABLE_NODES: 496
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/capture/simpleCapturingInClass.1.kt
 */

// FILE: a.kt
package foo

fun testAll(): String {
    val inlineX = InlineAll()

    return inlineX.inline({ a1: Int, a2: Double, a3: Double, a4: String, a5: Long ->
                              "" + a1 + a2 + a3 + a4 + a5},
                          1, 12.5, 13.5, "14", 15)
}

fun testAllWithCapturedVal(): String {
    val inlineX = InlineAll()

    val c1 = 21
    val c2 = 22.5
    val c3 = 23.5
    val c4 = "24"
    val c5 = 25
    val c6 = 'H'
    val c7 = 26
    val c8 = 27
    val c9 = 28.5

    return inlineX.inline({ a1: Int, a2: Double, a3: Double, a4: String, a5: Long ->
                              "" + a1 + a2 + a3 + a4 + a5 + c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9},
                          1, 12.5, 13.5, "14", 15)
}

fun testAllWithCapturedVar(): String {
    val inlineX = InlineAll()

    var c1 = 21
    var c2 = 22.5
    var c3 = 23.5
    var c4 = "24"
    var c5 = 25
    var c6 = 'H'
    var c7 = 26
    var c8 = 27
    val c9 = 28.5

    return inlineX.inline({ a1: Int, a2: Double, a3: Double, a4: String, a5: Long ->
                              "" + a1 + a2 + a3 + a4 + a5 + c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9},
                          1, 12.5, 13.5, "14", 15)
}

fun testAllWithCapturedValAndVar(): String {
    val inlineX = InlineAll()

    var c1 = 21
    var c2 = 22.5
    val c3 = 23.5
    val c4 = "24"
    var c5 = 25
    val c6 = 'H'
    var c7 = 26
    var c8 = 27
    val c9 = 28.5

    return inlineX.inline({ a1: Int, a2: Double, a3: Double, a4: String, a5: Long ->
                              "" + a1 + a2 + a3 + a4 + a5 + c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9},
                          1, 12.5, 13.5, "14", 15)
}


fun box(): String {
    if (testAll() != "112.513.51415") return "testAll: ${testAll()}"
    if (testAllWithCapturedVal() != "112.513.514152122.523.52425H262728.5") return "testAllWithCapturedVal: ${testAllWithCapturedVal()}"
    if (testAllWithCapturedVar() != "112.513.514152122.523.52425H262728.5") return "testAllWithCapturedVar: ${testAllWithCapturedVar()}"
    if (testAllWithCapturedValAndVar() != "112.513.514152122.523.52425H262728.5") return "testAllWithCapturedVal: ${testAllWithCapturedValAndVar()}"
    return "OK"
}


// FILE: b.kt
package foo

class InlineAll {

    inline fun inline(s: (Int, Double, Double, String, Long) -> String,
                      a1: Int, a2: Double, a3: Double, a4: String, a5: Long): String {
        return s(a1, a2, a3, a4, a5)
    }
}