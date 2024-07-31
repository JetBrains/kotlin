// EXPECTED_REACHABLE_NODES: 1280
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/simple/severalUsage.1.kt
 */

package foo

public inline fun <R> runTest(f: () -> R): R {
    return f()
}

public inline fun <R> minByTest(f: (Int) -> R): R {
    var minValue = f(1)
    val v = f(1)
    return v
}
// CHECK_BREAKS_COUNT: function=box count=0
// CHECK_LABELS_COUNT: function=box name=$l$block count=0
fun box(): String {
    val result = runTest{minByTest<Int> { it }}

    if (result != 1) return "test1: ${result}"

    return "OK"
}